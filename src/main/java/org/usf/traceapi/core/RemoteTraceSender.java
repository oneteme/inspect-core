package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	public static final String INCOMING_ENDPOINT = "incoming/request";
	public static final String OUTCOMING_ENDPOINT = "outcoming/request";

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();

	private final String host;
	private final int delay;
	private final TimeUnit unit;
	private final RestTemplate template;
	
	public RemoteTraceSender(TraceConfig config) {
		this(normalizeHost(config.getHost()), config.getDelay(), TimeUnit.valueOf(config.getUnit()), new RestTemplate());
	}

	@Override
	public void send(Metric trc) {
		var uri = join("/", host, TRACE_ENDPOINT, endpointFor(trc));
		executor.schedule(()-> template.put(uri, trc), delay, unit); //wait for sending response
	}
	
	private static String endpointFor(@NonNull Metric trc) {
		if(trc.getClass() == IncomingRequest.class) {
			return INCOMING_ENDPOINT;
		}
		else if(trc.getClass() ==  OutcomingRequest.class) { //super after
			return OUTCOMING_ENDPOINT;
		}
		throw new UnsupportedOperationException(trc.getClass().getSimpleName() + " : " + trc);
	}
	
	private static String normalizeHost(String host) {
		return host.endsWith("/") ? host.substring(0, host.length()-1) : host;
	}
}
