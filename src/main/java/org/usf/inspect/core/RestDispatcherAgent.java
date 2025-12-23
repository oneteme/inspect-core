package org.usf.inspect.core;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.InspectContext.context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
public final class RestDispatcherAgent implements DispatcherAgent {

	private final RestRemoteServerProperties properties;
	private final ObjectMapper mapper;
	private final RestTemplate template;
	private int attempts;

	private InstanceEnvironment instance;
	private boolean registred;

	public RestDispatcherAgent(RestRemoteServerProperties properties, ObjectMapper mapper) {
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
					.queryParamIfPresent ("end", complete ? Optional.of(now()) : empty())
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
	public void dispatch(File dumpFile) {
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
	
	String getOrRegisterInstanceId() {
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
	boolean shouldRetry(RestClientException e) {
		if(e instanceof HttpServerErrorException rsp) {
			try { //internal server error or Service unavailable
				var resp = mapper.readValue(rsp.getResponseBodyAsByteArray(), TraceFail.class);
				if(nonNull(resp) && resp.retry()) {
					return true; //retry only if server ask for it
				}
			} catch (IOException ioe) {/*ignore this exception */}
			context().reportError(false, "RestDispatcherAgent.shouldRetry", e);
			return false; //BadGateway or GatewayTimeout should not be retried
		}
		else if(e instanceof ResourceAccessException rae 
				&& rae.getCause() instanceof SocketTimeoutException 
				&& !rae.getMessage().contains("Connection timed out")) {
			context().reportError(false, "RestDispatcherAgent.shouldRetry", e);
			return false; //only read timeout should not be retried
		}
		log.warn("bad request : {}", e.getMessage());
		return true;
	}

	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, ObjectMapper mapper) {
		var json = new MappingJackson2HttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
		var rt = new RestTemplateBuilder()
				.messageConverters(json, plain) //minimum converters
				.setConnectTimeout(ofSeconds(30))
				.setReadTimeout(ofSeconds(60))
//				.additionalInterceptors(new HttpRequestInterceptor())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
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
					context().reportError(false, "RestDispatcherAgent.bodyCompressionInterceptor", e);
				}
			}
			return exec.execute(req, body);
		};
	}
}
