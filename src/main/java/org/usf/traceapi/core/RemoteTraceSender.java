package org.usf.traceapi.core;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RemoteTraceSender implements TraceSender {

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();

	private final String url;
	private final int delay;
	private final TimeUnit unit;
	private final RestTemplate template;
	
	public RemoteTraceSender(TraceConfig config) {
		this(config.getUrl(), config.getDelay(), TimeUnit.valueOf(config.getUnit()), new RestTemplate());
	}

	@Override
	public void send(Metric trc) {
		executor.schedule(()-> template.put(url, trc), delay, unit); //wait for sending response
	}
	
}
