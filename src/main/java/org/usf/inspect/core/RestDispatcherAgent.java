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
import static org.usf.inspect.core.ErrorReporter.reportError;

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

	private InstanceEnvironment instance;
	private boolean registred = false;

	public RestDispatcherAgent(RestRemoteServerProperties properties, ObjectMapper mapper) {
		this(properties, mapper, defaultRestTemplate(properties, mapper));
	}

	@Override
	public void dispatch(InstanceEnvironment instance) {
		this.instance = instance; //register on next dispatch
	}

	@Override
	public List<EventTrace> dispatch(boolean complete, int attempts, int pending, List<EventTrace> traces)  {
		assertInstanceRegistred();
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("attempts", attempts)
					.queryParam("pending", pending)
					.queryParamIfPresent ("end", complete ? Optional.of(now()) : empty())
					.buildAndExpand(instance.getId()).toUri();
			template.put(uri, traces.toArray(EventTrace[]::new)); //issue https://github.com/FasterXML/jackson-core/issues/1459
			return emptyList(); //no partial dispatch
		}
		catch (RestClientException e) { //server / client ?
			if(shouldRetry(e)) {
				throw new DispatchException("traces dispatch error", e);
			} //else may be lost
			return emptyList();
		}
	}
	
	@Override
	public void dispatch(int attempts, File dumpFile) {
		assertInstanceRegistred();
		try {
			var uri = fromUriString(properties.getTracesURI())
					.queryParam("attempts", attempts)
					.queryParam("filename", dumpFile.getName())
					.buildAndExpand(instance.getId()).toUri();
			template.put(uri, mapper.readTree(dumpFile)); //use dispatch splitor
		}
		catch (RestClientException e) { //server / client ?
			if(shouldRetry(e)) {
				throw new DispatchException("dump file dispatch error", e);
			} //else may be lost
		}
		catch (IOException e) {
			throw new DispatchException("dump file read error " + dumpFile, e);
		}
	}
	
	void assertInstanceRegistred() {
		if(!registred) {
			if(nonNull(instance)) {
				try {
					template.postForObject(properties.getInstanceURI(), instance, String.class);
					registred = true;
					log.info("instance was registred with id={}", instance.getId());
				}
				catch(RestClientException e) {//server / client ?
					throw new DispatchException("instance register error", e);
				}
			}
			else {
				throw new DispatchException("instance is null");
			}
		}
	}

	boolean shouldRetry(RestClientException e) {
		if(e instanceof HttpServerErrorException rsp) {
			try {
				var resp = mapper.readValue(rsp.getResponseBodyAsByteArray(), TraceFail.class);
				if(nonNull(resp) && resp.retry()) {
					return true;
				}
			} catch (IOException ioe) {/*ignore this exception */}
			reportError("RestDispatcherAgent.shouldRetry", null, e);
			return false;
		}
		else if(e instanceof ResourceAccessException rae && rae.getCause() instanceof SocketTimeoutException) {
			reportError("RestDispatcherAgent.shouldRetry", null, e);
			return false; //timeout should not be retried
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
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
		if(properties.getCompressMinSize() > 0) {
			rt = rt.interceptors(bodyCompressionInterceptor(properties));
		}
		return rt.build();
	}

	static ClientHttpRequestInterceptor bodyCompressionInterceptor(final RestRemoteServerProperties properties) {
		return (req, body, exec)->{
			if(body.length >= properties.getCompressMinSize()) {
				var baos = new ByteArrayOutputStream();
				try (var gos = new GZIPOutputStream(baos)) {
					gos.write(body);
					req.getHeaders().add(CONTENT_ENCODING, "gzip");
					body = baos.toByteArray();
				}
				catch (Exception e) {/*do not throw exception */
					reportError("RestDispatcherAgent.bodyCompressionInterceptor", null, e);
				}
			}
			return exec.execute(req, body);
		};
	}
}
