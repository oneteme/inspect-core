package org.usf.traceapi.core;

import static java.time.Duration.ofSeconds;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.logSessions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.netty.handler.codec.http2.Http2Headers;
import jakarta.activation.MimeType;

/**
 * 
 * @author u$f
 *
 */
public final class RemoteTraceSender implements TraceHandler {
	
	private final TraceConfigurationProperties properties;
	private final RestTemplate template;
	private final InstanceEnvironment application;
	private final ScheduledDispatcher<Session> dispatcher;
	private String instanceId;

	public RemoteTraceSender(TraceConfigurationProperties properties, InstanceEnvironment application) {
		this(properties, application, createRestTemplate());
	}
	
	public RemoteTraceSender(TraceConfigurationProperties properties, InstanceEnvironment application, RestTemplate template) {
		this.properties = properties;
		this.template = template;
		this.application = application;
		this.dispatcher = new ScheduledDispatcher<>(properties, this::send, Session::completed);
		tryRegisterServer();
	}
	
	void tryRegisterServer() {
		if(isNull(instanceId)) {
			try {
				instanceId = template.postForObject(properties.instanceApiURL(), application, String.class);
			}
			catch (Exception e) {
				log.warn("cannot register instance=" + application, e);
			}
		}
	}
	
	@Override
	public void handle(Session session) {
		dispatcher.add(session);
	}
	
    private boolean send(int attemps, List<Session> sessions) {
    	tryRegisterServer(); //if not registered before
    	if(nonNull(instanceId)) {
    		logSessions(sessions);
    		template.put(properties.sessionApiURL(), sessions.toArray(Session[]::new), instanceId);
    		return true;
    	}
    	return false;
    }

	private static RestTemplate createRestTemplate() {
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
		    catch (Exception e) {/*do not throw exception */}
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
