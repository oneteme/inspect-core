package org.usf.inspect.core;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.core.Helper.log;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class EventTraceRestDispatcher implements EventTraceDispatcher<EventTrace> {
	
	private final RestRemoteServerProperties properties;
	private final InstanceEnvironment application;
	private final RestTemplate template;
	private String instanceId;

	public EventTraceRestDispatcher(RestRemoteServerProperties properties, InstanceEnvironment application) {
		this(properties, application, defaultRestTemplate(properties));
	}
	
	@Override
    public boolean dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces) {
		if(isNull(instanceId)) {//if not registered before
			try {
				log.info("registering instance: {}", application);
				instanceId = template.postForObject(properties.getInstanceEndpoint(), application, String.class);
			}
			catch(Exception e) {
				log.warn("cannot register instance, cause: [{}] {}", e.getClass().getSimpleName(), e.getMessage());
				throw e;
			}
		}
    	if(nonNull(instanceId)) {
			template.put(properties.getSessionEndpoint(), traces.toArray(EventTrace[]::new), instanceId, attemps, pending, complete ? now() : null);
			return true; //return true to remove items from the queue
    	}
    	return false; //add back items back to the queue
    }
	
	static RestTemplate defaultRestTemplate(RestRemoteServerProperties properties) {
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
	
	static ClientHttpRequestInterceptor compressRequest(final RestRemoteServerProperties properties) {
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
		mapper.registerSubtypes(
				new NamedType(LogEntry.class, 				"log"),  
				new NamedType(MachineResourceUsage.class, 	"rsrc-usg"),
				new NamedType(MainSession.class,  			"main-ses"), 
				new NamedType(RestSession.class,  			"rest-ses"), 
				new NamedType(LocalRequest.class, 			"locl-req"), 
				new NamedType(DatabaseRequest.class,		"jdbc-req"),
				new NamedType(RestRequest.class,  			"http-req"), 
				new NamedType(MailRequest.class,  			"mail-req"), 
				new NamedType(NamingRequest.class,			"ldap-req"), 
				new NamedType(FtpRequest.class,  			"ftp-req"),
				new NamedType(DatabaseRequestStage.class,	"jdbc-stg"),
				new NamedType(HttpRequestStage.class,  		"http-stg"), 
				new NamedType(MailRequestStage.class,  		"mail-stg"), 
				new NamedType(NamingRequestStage.class,		"ldap-stg"), 
				new NamedType(FtpRequestStage.class,  		"ftp-stg"));
		return mapper;
	}
}
