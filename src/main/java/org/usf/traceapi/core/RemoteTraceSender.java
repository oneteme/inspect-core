package org.usf.traceapi.core;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 
 * @author u$f
 *
 */
public final class RemoteTraceSender implements TraceHandler {
	
	private final TraceConfigurationProperties properties;
	private final RestTemplate template;
	private final ScheduledSessionDispatcher dispatcher;

	public RemoteTraceSender(TraceConfigurationProperties properties) {
		this(properties, createRestTemplate());
	}
	
	public RemoteTraceSender(TraceConfigurationProperties prop, RestTemplate template) {
		this.properties = prop;
		this.template = template;
		this.dispatcher = new ScheduledSessionDispatcher(prop, Session::wasCompleted, this::sendCompleted);
	}
	
	@Override
	public void handle(Session session) {
		dispatcher.add(session);
	}
	
    private boolean sendCompleted(int attemps, List<? extends Session> sessions) {
		template.put(properties.getUrl(), sessions);
		return true;
    }

	private static RestTemplate createRestTemplate() {
		var convert = new MappingJackson2HttpMessageConverter(createObjectMapper());
	    var timeout = ofSeconds(30);
	    return new RestTemplateBuilder()
	    		.messageConverters(singletonList(convert))
				.setConnectTimeout(timeout)
				.setReadTimeout(timeout)
				.build();
	}
	
	private static ObjectMapper createObjectMapper() {
	     ObjectMapper mapper = new ObjectMapper();
	     mapper.registerModule(new JavaTimeModule()); //new ParameterNamesModule() not required
//	     mapper.disable(WRITE_DATES_AS_TIMESTAMPS) important! write Instant as double
	     return mapper;
	}
}
