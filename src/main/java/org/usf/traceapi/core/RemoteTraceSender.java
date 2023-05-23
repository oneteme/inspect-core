package org.usf.traceapi.core;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RemoteTraceSender implements TraceSender {

	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();

	private final String url;
	private final int delay;
	private final TimeUnit unit;
	private final RestTemplate template;
	
	public RemoteTraceSender(String url, int delay, TimeUnit unit) {
		this(url, delay, unit, new RestTemplate());
	}

	@Override
	public void send(IncomingRequest mr) {
		executor.schedule(()-> template.put(url, mr), delay, unit); //wait for sending response
	}
	
}
