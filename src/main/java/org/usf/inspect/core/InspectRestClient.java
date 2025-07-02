package org.usf.inspect.core;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.core.Helper.log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.ScheduledDispatchHandler.Dispatcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class InspectRestClient implements Dispatcher<Metric> {
	
	private final RestClientProperties properties;
	private final InstanceEnvironment application;
	private final RestTemplate template;
	private String instanceId;

	public InspectRestClient(RestClientProperties properties, InstanceEnvironment application) {
		this(properties, application, defaultRestTemplate(properties));
	}
	
	@Override
    public List<Metric> dispatch(boolean complete, int attemps, List<Metric> metrics) {
		if(isNull(instanceId)) {//if not registered before
			try {
				log.info("registering instance: {}", application);
				instanceId = template.postForObject(properties.getInstanceApi(), application, String.class);
			}
			catch(RestClientException e) {
				log.warn("cannot register instance, cause: [{}] {}", e.getClass().getSimpleName(), e.getMessage());
				throw e;
			}
		}
    	if(nonNull(instanceId)) {
    		List<Metric> pdn = null;
    		try {
    			pdn = extractPendingMetrics(metrics);
    			if(!pdn.isEmpty()) {
					log.info("{} pending metrics, will be not send", pdn.size());
				}
    			template.put(properties.getSessionApi(), metrics.toArray(Metric[]::new), instanceId, attemps, pdn.size(), complete ? now() : null);
    			metrics = new ArrayList<>(); //release memory
    		}
    		finally {
				if(nonNull(pdn)) {
					metrics.addAll(pdn);
				}
			}
    	}
    	return metrics;
    }
	
	private List<Metric> extractPendingMetrics(List<Metric> metrics) {
		var pending = new ArrayList<Metric>();
		var now = now();
		var lazyAfter = properties.getLazyAfter();
		for(var it=metrics.listIterator(); it.hasNext();) {
			var o = it.next();
			o.lazy(()->{
				if(isNull(o.getEnd())) {
					if(o.getStart().until(now, SECONDS) > lazyAfter) {
						it.set(o.copy()); //do not put it in pending, will be sent later
					}
					else {
						pending.add(o);
						it.remove();
					}
				}
			});
		}
		return pending;
	}
	
	static RestTemplate defaultRestTemplate(RestClientProperties properties) {
		var json = new MappingJackson2HttpMessageConverter(createObjectMapper());
		var plain = new StringHttpMessageConverter(); //for instanceID
	    var timeout = ofSeconds(600); //wait for server startup 
	    var rt = new RestTemplateBuilder()
	    		.messageConverters(json, plain)
				.setConnectTimeout(timeout)
				.setReadTimeout(timeout)
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
	    if(properties.getCompressMinSize() > 0) {
	    	rt = rt.interceptors(compressRequest(properties));
	    }
	    return rt.build();
	}
	
	static ClientHttpRequestInterceptor compressRequest(final RestClientProperties properties) {
		return (req, body, exec)->{
			if(body.length >= properties.getCompressMinSize()) {
			    var baos = new ByteArrayOutputStream();
			    try (var gos = new GZIPOutputStream(baos)) {
			        gos.write(body);
				    req.getHeaders().add(CONTENT_ENCODING, "gzip");
			        body = baos.toByteArray();
			    }
			    catch (Exception e) {/*do not throw exception */
			    	log.warn("cannot compress sessions, {}", e.getMessage());
			    }
			}
	    	return exec.execute(req, body);
		};
	}
	
	private static ObjectMapper createObjectMapper() {
	     var mapper = new ObjectMapper();
	     mapper.registerModule(new JavaTimeModule()); //new ParameterNamesModule() not required
	     mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//	     mapper.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
	     return mapper;
	}
}
