package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.encodeBasicAuth;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.web.WebUtils.TRACE_RETRY_HEADER;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.usf.inspect.EventTrace;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.RestRemoteServerProperties;
import org.usf.inspect.web.httpclient.HttpRequestInterceptor;

import jakarta.annotation.Nonnull;
import tools.jackson.databind.json.JsonMapper;

/**
 * 
 * @NotThreadSafe
 * 
 * @author u$f
 *
 */
public final class RestClientTracePublisher implements TracePublisher {
	
	private final RestTemplate template;
	private final UriComponentsBuilder instanceUriBuilder;
	private final UriComponentsBuilder traceUriBuilder;
	
	private InstanceEnvironment instance;
	private boolean registered;
	private int attempts;
	private int sequence;
	private EventTrace[] pendingBatch;

	public RestClientTracePublisher(RestRemoteServerProperties properties, JsonMapper.Builder mapper, String namespace, boolean debug) {
		this(properties, defaultRestTemplate(properties, mapper, namespace, debug));
	}
	
	public RestClientTracePublisher(RestRemoteServerProperties properties, RestTemplate template) {
		this.template = template;
		this.instanceUriBuilder = fromUriString(properties.getInstanceURI());
		this.traceUriBuilder = fromUriString(properties.getTracesURI());
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
	public void flush(boolean complete, ProcessingQueue<EventTrace> queue) throws TraceDispatchException {
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
			if(!snp.isEmpty()) {
				mergeTraceParts(snp);
			} //heartbeat, flush even if empty
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
			else {
				attempts = 0;
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
				log.warn("pending batch is not flushed, {} traces will be aborted", pendingBatch.length);
				pendingBatch = null;
				attempts = 0;
			}
		}
	}
	
	URI buildTraceUri(UUID id, int seq, int atm, boolean complete) {
	    return traceUriBuilder
	    		.replaceQueryParam("seq", seq)
	            .replaceQueryParam("atm", atm)
	            .replaceQueryParam("end", complete ? systemUTC().instant() : null)
	            .buildAndExpand(id)
	            .toUri();
	}

	UUID ensureInstanceRegistered() throws TraceDispatchException {
		if(registered) {
			return instance.getId();
		}
		if(nonNull(instance)) {
			var uri = instanceUriBuilder.replaceQueryParam("atm", ++attempts).build().toUri();
			try {
				template.put(uri, instance);
				registered = true;
				attempts = 0;
				log.info("instance registered successfully, id={}", instance.getId());
				return instance.getId();
			}
			catch(HttpClientErrorException e) {
				if(e.getStatusCode() == CONFLICT) { //409 : instance already registered, ignore
					registered = true;
					attempts = 0;
					log.info("instance already registered, id={}", instance.getId());
					return instance.getId();
				}
				reportError("RestClientTracePublisher.ensureInstanceRegistered: failed to register instance, attempt={}: status={}", attempts, e.getStatusCode(), e);
			}
			catch(RestClientException e) {//server / client ?
				reportError("RestClientTracePublisher.ensureInstanceRegistered: failed to register instance, attempt={}", attempts, e);
			}
		}
		else { //should not happen, register() should be called before flush()
			log.warn("instance environment is not set, cannot register instance");
		}
		throw new TraceDispatchException("failed to register instance, attempt #"+attempts);
	}

	boolean shouldRetry(RestClientException e) {
		Object status = null;
		if(e instanceof RestClientResponseException rst) {
			var hdr = rst.getResponseHeaders();
			if(nonNull(hdr) && hdr.containsHeader(TRACE_RETRY_HEADER)) {
				return RETRY.equals(hdr.getFirst(TRACE_RETRY_HEADER)); //server synchronization 
			}
			var stt = rst.getStatusCode(); //404 : kube ingress during server rollout
			if(stt.is4xxClientError() && status != NOT_FOUND && status != REQUEST_TIMEOUT && status != TOO_MANY_REQUESTS) {
				if(status != CONFLICT) { //409 : sequence already registered, ignore
					reportError("RestClientTracePublisher.shouldRetry: failed to publish traces and will not retry, attempt={}, status={}", attempts, status, e);
				}
				return false;
			}
			status = stt;
		}
		else {
			status = "ERROR";
		}
		reportError("RestClientTracePublisher.shouldRetry: failed to publish traces and will retry, attempt={}, status={}", attempts, status, e);
		return true;
	}

	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties, JsonMapper.Builder mapper, String namespace, boolean debug) {
		var json = new JacksonJsonHttpMessageConverter(mapper);
		var plain = new StringHttpMessageConverter(); //for instanceID
		var bldr = new RestTemplateBuilder()
				.messageConverters(json, plain) //minimum converters
				.connectTimeout(ofSeconds(10))
				.readTimeout(ofSeconds(30))
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultHeader(AUTHORIZATION, "Basic " + encodeBasicAuth(namespace, properties.getToken(), null));
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
	
	void reportError(String msg, Object... args) {
		if(attempts % 10 == 1) { 
			REPORTER.error(msg, args); //inspect report for alerting, will be repeated every 10 attempts
		}
		if(attempts % 50 == 1) {
			log.warn(msg, args); //standard log for alerting, will be repeated every 50 attempts
		}
	}
}