package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.usf.traceapi.core.Helper.log;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.web.client.RestTemplate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RemoteTraceSender implements TraceSender {
	
	public static final String TRACE_ENDPOINT = "trace";
	public static final String MAIN_ENDPOINT = "main/request";
	public static final String INCOMING_ENDPOINT = "incoming/request";

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();

	private final TraceConfigurationProperties properties;
	private final RestTemplate template;
	
	public RemoteTraceSender(TraceConfigurationProperties properties) {
		this(properties, new RestTemplate());
	}
	
	@Override
	public void send(Session session) {
		var uri = join("/", properties.getHost(), TRACE_ENDPOINT, endpointFor(session));
		log.debug("sending trace {} => {}", session.getId(), uri);
		executor.schedule(()-> template.put(uri, session), properties.getDelay(), properties.getUnit()); //wait for sending response
	}
	
	private static String endpointFor(@NonNull Session session) {
		if(session.getClass() == IncomingRequest.class) {
			return INCOMING_ENDPOINT;
		}
		else if(session.getClass() == MainRequest.class) {
			return MAIN_ENDPOINT;
		}
		throw new UnsupportedOperationException(session.getClass().getSimpleName() + " : " + session);
	}
}
