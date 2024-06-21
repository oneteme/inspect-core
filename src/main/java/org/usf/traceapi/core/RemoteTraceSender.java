package org.usf.traceapi.core;

import static java.time.Duration.ofSeconds;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.traceapi.core.Helper.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.usf.traceapi.core.ScheduledDispatchHandler.Dispatcher;

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
public final class RemoteTraceSender implements Dispatcher<Session> {
	
	private final RemoteTracerProperties properties;
	private final InstanceEnvironment application;
	private final RestTemplate template;
	private String instanceId;

	public RemoteTraceSender(RemoteTracerProperties properties, InstanceEnvironment application) {
		this(properties, application, defaultRestTemplate());
	}
	
	@Override
    public boolean dispatch(boolean complete, int attemps, List<Session> sessions) {
		if(isNull(instanceId)) {//if not registered before
			try {
				instanceId = template.postForObject(properties.getInstanceApi(), application, String.class);
			}
			catch (Exception e) {
				log.warn("cannot register instance, {}", e.getMessage());
			}
		} 
    	if(nonNull(instanceId)) {
    		template.put(properties.getSessionApi(), sessions.toArray(Session[]::new), instanceId);
    		return true;
    	}
    	return false;
    }

	private static RestTemplate defaultRestTemplate() {
		var json = new MappingJackson2HttpMessageConverter(createObjectMapper());
		var plain = new StringHttpMessageConverter(); //instanceID
	    var timeout = ofSeconds(30);
	    return new RestTemplateBuilder()
	    		.interceptors(RemoteTraceSender::compressRequest)
	    		.messageConverters(json, plain)
				.setConnectTimeout(timeout)
				.setReadTimeout(timeout)
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}
	
	public static ClientHttpResponse compressRequest(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
		if(body.length >= 5_000) { //over 5Ko config ?
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
	}
	
	private static ObjectMapper createObjectMapper() {
	     var mapper = new ObjectMapper();
	     mapper.registerModule(new JavaTimeModule()); //new ParameterNamesModule() not required
	     mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY); //v22
//	     mapper.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
	     return mapper;
	}
}
