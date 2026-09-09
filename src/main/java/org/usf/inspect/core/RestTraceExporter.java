package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.encodeBasicAuth;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
	private final ObjectMapper mapper;
	private final RestTemplate template;
	private int attempts;

	private InstanceEnvironment instance;
	private boolean registred;

	public RestTraceExporter(RestRemoteServerProperties properties, ObjectMapper mapper) {
		this(properties, mapper, defaultRestTemplate(properties, mapper));
	}

	@Override
	public void dispatch(InstanceEnvironment instance) {
		this.instance = instance; //register on next dispatch
	}

	@Override
	public List<EventTrace> dispatch(boolean complete, List<EventTrace> traces)  {
		var id = getOrRegisterInstanceId();
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("attempts", ++attempts)
					.queryParamIfPresent("end", complete ? Optional.of(systemUTC().instant()) : empty())
					.buildAndExpand(id).toUri();
			template.put(uri, traces.toArray(EventTrace[]::new)); //issue https://github.com/FasterXML/jackson-core/issues/1459
			attempts = 0;
			return emptyList(); //no partial dispatch
		}
		catch (RestClientException e) { //server / client ?
			if(shouldRetry(e)) {
				throw new DispatchException("traces dispatch error", e);
			} //else may be lost
			log.warn("dispatching {} traces failed, will not retry", traces.size());
			return emptyList();
		}
	}
	
	@Override
	@Deprecated(forRemoval = true, since = "v1.2")
	public void dispatch(File dumpFile) { //TD send FileSystemResource ?
		var id = getOrRegisterInstanceId();
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("attempts", attempts)
					.queryParam("filename", dumpFile.getName())
					.buildAndExpand(id).toUri();
			template.put(uri, mapper.readTree(dumpFile)); //use dispatch splitor
		}
		catch (RestClientException e) { //server / client ?
			if(shouldRetry(e)) {
				throw new DispatchException("file dispatch error", e);
			} //else may be lost
			log.warn("file dispatch failed, will not retry {}", dumpFile);
		}
		catch (IOException e) {
			throw new DispatchException("file dispatch error", e);
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
	boolean shouldRetry(RestClientException e) throws UnknownExportState {
		if(e instanceof HttpServerErrorException rsp) {
			var body = rsp.getResponseBodyAsByteArray();
			if(nonNull(body) && body.length > 0) {
				try {
					var resp = mapper.readValue(body, TraceFail.class);
					if(nonNull(resp)) {
						return resp.retry(); //server response
					}
				} catch (IOException ioe) {
					throw new UnknownExportState("cannot read server response body: " + new String(body));
				}
			}
			throw new UnknownExportState("server response body is empty");
		}
		if(e instanceof HttpClientErrorException rsp) { //40x : BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT
			return rsp.getStatusCode() == TOO_MANY_REQUESTS; //retry after delay !
		}
		else if(e instanceof ResourceAccessException rae 
				&& rae.getCause() instanceof SocketTimeoutException 
				&& !rae.getCause().getMessage().contains("Connection timed out")) {
			hub().reportError("RestTraceExporter.shouldRetry", e);
			throw new UnknownExportState("read timeout");
		}
		return true;
	}

	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, ObjectMapper mapper) {
		var json = new MappingJackson2HttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
		var rt = new RestTemplateBuilder()
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
}
