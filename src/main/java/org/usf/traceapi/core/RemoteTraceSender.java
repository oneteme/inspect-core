package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

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
	public static final String OUTCOMING_ENDPOINT = "outcoming/request";

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();

	private final TraceConfigurationProperties properties;
	private final RestTemplate template;
	
	public RemoteTraceSender(TraceConfigurationProperties properties) {
		this(properties, new RestTemplate());
	}
	
	@Override
	public void send(Metric metric) {
		var uri = join("/", properties.getHost(), TRACE_ENDPOINT, endpointFor(metric));
		executor.schedule(()-> template.put(uri, metric), properties.getDelay(), properties.getUnit()); //wait for sending response
	}
	
	private static String endpointFor(@NonNull Metric metric) {
		if(metric.getClass() == IncomingRequest.class) {
			return INCOMING_ENDPOINT;
		}
		else if(metric.getClass() == MainRequest.class) {
			return MAIN_ENDPOINT;
		}
		else if(metric.getClass() == OutcomingRequest.class) {
			return OUTCOMING_ENDPOINT;
		}
		throw new UnsupportedOperationException(metric.getClass().getSimpleName() + " : " + metric);
	}
}
