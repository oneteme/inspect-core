package org.usf.inspect.core;

import static java.lang.Integer.parseInt;
import static java.time.Clock.systemUTC;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpHeaders.encodeBasicAuth;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.http.HttpRequestInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class RestTraceExporter implements TraceExporter {

	private final RestRemoteServerProperties properties;
	private final RestTemplate template;
	private int attempts;
	private int sequence;
	
	private EventTrace[] lastPacket;
	private InstanceEnvironment instance;
	private boolean registred;

	public RestTraceExporter(RestRemoteServerProperties properties, ObjectMapper mapper) {
		this(properties, defaultRestTemplate(properties, mapper));
	}

	@Override
	public void dispatch(InstanceEnvironment instance) {
		this.instance = instance; //register on next dispatch
	}

	@Override
	public List<EventTrace> dispatch(boolean complete, List<EventTrace> traces)  {
		var id = getOrRegisterInstanceId();
		if(nonNull(lastPacket)) {
			dispatchPrevious(id);
			if(nonNull(lastPacket)) {
				return traces;
			}
		}
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("seq", ++sequence) 
					.queryParam("atm", ++attempts)
					.queryParamIfPresent("end", complete ? Optional.of(systemUTC().instant()) : empty())
					.buildAndExpand(id).toUri();
			template.put(uri, traces.toArray(EventTrace[]::new)); //issue https://github.com/FasterXML/jackson-core/issues/1459
			attempts = 0;
			return emptyList();
		}
		catch (RestClientException e) {
			try {
				return shouldRetry(e) ? traces : emptyList();
			}
			catch (UnconfirmedExportException ex) {
				lastPacket = traces.toArray(EventTrace[]::new);
				return emptyList();
			}
		}
	}
	
	void dispatchPrevious(UUID id) {
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("seq", sequence) //same sequence
					.queryParam("atm", ++attempts)
					.buildAndExpand(id).toUri();
			template.put(uri, lastPacket); //issue https://github.com/FasterXML/jackson-core/issues/1459
			attempts = 0;
			lastPacket = null;
		}
		catch (RestClientException e) { //server / client ?
			try {
				if(!shouldRetry(e)) {
					lastPacket = null;
				}
			} catch (UnconfirmedExportException e1) {
				//keep last packet
			}
			finally {
				if(attempts > 10) {
					log.warn("dispatching {} traces failed, will not retry", lastPacket.length);
					lastPacket = null;
					attempts = 0;
				}
			}
		}
	}

	UUID getOrRegisterInstanceId() {
		if(registred) {
			return instance.getId();
		}
		if(nonNull(instance)) {
			try {
				++attempts;
				template.postForObject(properties.getInstanceURI(), instance, String.class);
				registred = true;
				attempts = 0;
				log.info("instance was registred with id={}", instance.getId());
				return instance.getId();
			}
			catch(RestClientException e) {//server / client ?
				throw new DispatchException("instance registration failed", e);
			}
		}
		throw new DispatchException("instance environment not set");
	}

	//see https://www.baeldung.com/java-socket-connection-read-timeout
	boolean shouldRetry(RestClientException e) throws UnconfirmedExportException {
		if(e instanceof HttpServerErrorException rsp) { //50x check header !?
			var hdr = rsp.getResponseHeaders();
			if(nonNull(hdr) && hdr.containsKey(RETRY_AFTER)) {
				var retry = hdr.getFirst(RETRY_AFTER);
				try {
					return parseInt(retry) > 0;
				} catch (Exception ex) {
					hub().reportError("RestTraceExporter.shouldRetry", ex);
					throw new UnconfirmedExportException("cannot read 'RETRY_AFTER' header : " + retry);
				}
			}
			hub().reportError("RestTraceExporter.shouldRetry", e);
			throw new UnconfirmedExportException("header[RETRY_AFTER] is empty");
		}
		if(e instanceof HttpClientErrorException rsp) { //40x : BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT
			hub().reportError("RestTraceExporter.shouldRetry", e);
			return rsp.getStatusCode() == TOO_MANY_REQUESTS; //retry after delay !
		}
		else if(e instanceof ResourceAccessException rae 
				&& rae.getCause() instanceof SocketTimeoutException 
				&& nonNull(rae.getCause().getMessage())
				&& !rae.getCause().getMessage().toLowerCase().contains("connect")) { 
			hub().reportError("RestTraceExporter.shouldRetry", rae.getCause());
			throw new UnconfirmedExportException("read timeout");
		}
		hub().reportError("RestTraceExporter.shouldRetry", e);
		return true;
	}

	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, ObjectMapper mapper) {
		var json = new MappingJackson2HttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
		var rt = new RestTemplateBuilder()
				.interceptors(new HttpRequestInterceptor())	//debug mode
				.messageConverters(json, plain) //minimum converters
				.setConnectTimeout(ofSeconds(10))
				.setReadTimeout(ofSeconds(30))
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultHeader(AUTHORIZATION, "Basic " + encodeBasicAuth(properties.getNamespace(), properties.getToken(), null));
		if(properties.getCompressMinSize() > 0) {
			rt = rt.interceptors(bodyCompressionInterceptor(properties.getCompressMinSize()));
		}
		return rt.build();
	}

	static ClientHttpRequestInterceptor bodyCompressionInterceptor(int size) {
		return (req, body, exec)->{
			if(body.length >= size) {
				var baos = new ByteArrayOutputStream();
				try (var gos = new GZIPOutputStream(baos)) {
					gos.write(body);
					req.getHeaders().add(CONTENT_ENCODING, "gzip");
					body = baos.toByteArray();
				}
				catch (Exception e) {/*do not throw exception */
					hub().reportError("RestTraceExporter.bodyCompressionInterceptor", e);
				}
			}
			return exec.execute(req, body);
		};
	}
	
	@SuppressWarnings("serial")
	static class UnconfirmedExportException extends Exception {

		public UnconfirmedExportException(String msg) {
			super(msg);
		}
	}
}
