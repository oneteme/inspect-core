package org.usf.traceapi.core;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduledDispatchProperties {
	
    private int delay = 5;
	private TimeUnit unit = SECONDS;
	private int bufferSize = 100; // {n} sessions
	private int bufferMaxSize = 5_000; // {n} sessions, -1: unlimited

	void validate() {
		assertPositive(delay, "delay");
		assertPositive(bufferSize, "bufferSize");
	}

	private static int assertPositive(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException("trace." + name + "=" +  v + " <= 0");
	}
}
