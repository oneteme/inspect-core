package org.usf.traceapi.core;

import static java.time.Duration.ofSeconds;

import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

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
		this(properties, new RestTemplateBuilder()
				.setConnectTimeout(ofSeconds(30))
				.setReadTimeout(ofSeconds(30))
				.build());
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
}
