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
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.web.WebUtils.TRACE_RETRY_HEADER;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
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
import org.usf.inspect.EventTrace;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.RestRemoteServerProperties;
import org.usf.inspect.web.httpclient.HttpRequestInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class RestClientTracePublisher implements TracePublisher {

	private final RestRemoteServerProperties properties;
	private final RestTemplate template;
	private InstanceEnvironment instance;
	
	private boolean registered;
	private int attempts;
	private int sequence;
	private EventTrace[] pendingBatch;

	public RestClientTracePublisher(RestRemoteServerProperties properties, ObjectMapper mapper, boolean debug) {
		this(properties, defaultRestTemplate(properties, mapper, debug));
	}

	@Override
	public void register(@Nonnull InstanceEnvironment instance) {
		if(isNull(this.instance)) {
			this.instance = instance; //register on next dispatch
		}
		else {
			REPORTER.warn("RestClientTracePublisher.register: instance environment is already set, cannot override");
		}
	}

	@Override
	public void flush(boolean complete, ProcessingQueue<EventTrace> queue) throws DispatchException {
		var id = ensureInstanceRegistered();
		if(nonNull(pendingBatch)) {
			flushPendingBatch(id);
			if(nonNull(pendingBatch)) { //retry until complete or capacity exceeded
				if(!complete && queue.size() + pendingBatch.length < queue.getMaxCapacity()) {
					return; //break, will retry later
				}
				log.warn("pending batch is not flushed, {} traces will be aborted", pendingBatch.length);
			}
			pendingBatch = null;
			attempts = 0;
		}
		queue.pollAll(snp->{ 
			mergeTraceParts(snp);
			return flushTraces(id, complete, snp);
		});
	}
	
	List<EventTrace> flushTraces(UUID id, boolean complete, List<EventTrace> traces)  {
		var arr = traces.toArray(EventTrace[]::new);
		var uri = buildTraceUri(id, ++sequence, ++attempts, complete);
		try { //issue https://github.com/FasterXML/jackson-core/issues/1459
			template.put(uri, arr); 
			attempts = 0;
			return emptyList();
		}
		catch (RestClientException e) {
			if(shouldRetry(e)) {
				pendingBatch = arr; //will retry later, do not turn back to queue
			}
			return emptyList();
		}
	}
	
	void flushPendingBatch(UUID id) {
		var uri = buildTraceUri(id, sequence, ++attempts, false); //same sequence
		try { //issue https://github.com/FasterXML/jackson-core/issues/1459
			template.put(uri, pendingBatch);
			attempts = 0;
			pendingBatch = null;
		}
		catch (RestClientException e) {
			if(!shouldRetry(e)) { //abort
				pendingBatch = null;
				attempts = 0;
			}
		}
	}
	
	URI buildTraceUri(UUID id, int seq, int atm, boolean complete) {
	    return fromUriString(properties.getTracesURI())
	            .queryParam("seq", seq)
	            .queryParam("atm", atm)
	            .queryParamIfPresent("end", complete ? Optional.of(systemUTC().instant()) : empty())
	            .buildAndExpand(id)
	            .toUri();
	}

	UUID ensureInstanceRegistered() throws DispatchException {
		if(registered) {
			return instance.getId();
		}
		var uri = fromUriString(properties.getInstanceURI())
				.queryParam("atm", ++attempts).build().toUri();
		if(nonNull(instance)) {
			try {
				template.postForObject(uri, instance, String.class);
				registered = true;
				attempts = 0;
				log.info("instance registered successfully, id={}", instance.getId());
				return instance.getId();
			}
			catch(RestClientException e) {//server / client ?
				REPORTER.error("RestClientTracePublisher.ensureInstanceRegistered: failed to register instance, attempt #{}: {}", attempts, e);
			}
		}
		else {
			REPORTER.warn("RestClientTracePublisher.ensureInstanceRegistered: instance environment is not set, cannot register instance");
		}
		throw new DispatchException("failed to register instance, attempt #"+attempts);
	}

	boolean shouldRetry(RestClientException e) {
		if(e instanceof HttpClientErrorException rsp) { //40x : BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT
			REPORTER.error("RestClientTracePublisher.evaluateRetry: status={}", rsp.getStatusCode(), e);
			return rsp.getStatusCode() == TOO_MANY_REQUESTS; //retry only if server is overloaded, otherwise abort
		}
		if(e instanceof HttpServerErrorException rsp) { //50x check header !?
			var hdr = rsp.getResponseHeaders();
			if(nonNull(hdr) && hdr.containsKey(TRACE_RETRY_HEADER)) {
				return RETRY.equals(hdr.getFirst(TRACE_RETRY_HEADER));
			}
			REPORTER.error("RestClientTracePublisher.evaluateRetry: '{}' is null", TRACE_RETRY_HEADER, e);
		}//see https://www.baeldung.com/java-socket-connection-read-timeout
		if(e instanceof ResourceAccessException rae 
				&& rae.getCause() instanceof SocketTimeoutException cause) { 
			REPORTER.error("RestClientTracePublisher.evaluateRetry: timeout", cause);
		}
		else {
			REPORTER.error("RestClientTracePublisher.evaluateRetry", e);
		}
		return true;
	}

	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, ObjectMapper mapper, boolean debug) {
		var json = new MappingJackson2HttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
		var bldr = new RestTemplateBuilder()
				.messageConverters(json, plain) //minimum converters
				.setConnectTimeout(ofSeconds(10))
				.setReadTimeout(ofSeconds(30))
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultHeader(AUTHORIZATION, "Basic " + encodeBasicAuth(properties.getNamespace(), properties.getToken(), null));
		if(properties.getCompressMinSize() > 0) {
			log.info("body compression enabled, min size={} bytes", properties.getCompressMinSize());
			bldr = bldr.interceptors(bodyCompressionInterceptor(properties.getCompressMinSize()));
		}
		if(debug) {
			log.info("debug mode enabled, adding request interceptor");
			bldr = bldr.additionalInterceptors(new HttpRequestInterceptor());
		}
		return bldr.build();
	}

	static ClientHttpRequestInterceptor bodyCompressionInterceptor(int size) {
		return (req, body, exec)->{
			if(body.length >= size) {
				var baos = new ByteArrayOutputStream();
				try (var gos = new GZIPOutputStream(baos)) {
					gos.write(body);
					req.getHeaders().add(CONTENT_ENCODING, "gzip");
				}
				catch (Exception e) {/*do not throw exception */
					REPORTER.error("RestClientTracePublisher.bodyCompressionInterceptor: failed compressing body request", e);
					req.getHeaders().remove(CONTENT_ENCODING);
					baos.reset();
				}
				if(baos.size() > 0) {
					body = baos.toByteArray();
				}
			}
			return exec.execute(req, body);
		};
	}
}
