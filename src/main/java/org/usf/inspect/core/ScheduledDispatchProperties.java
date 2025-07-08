package org.usf.inspect.core;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScheduledDispatchProperties {
	
    private int delay = 5;
	private TimeUnit unit = SECONDS;
	@Deprecated(forRemoval = true, since = "1.1") // @see ThreadSafeQueue::queue 
	private int bufferSize = 100; // {n} traces
	private int bufferMaxSize = 5_000; // {n} traces, 0: unlimited
	private int lazyAfter = 30; // send lazy traces after {n} seconds, 0: send immediately

	void validate() {
		assertPositive(delay, "delay");
		assertPositive(bufferSize, "bufferSize");
		assertPositive(lazyAfter, "lazyAfter");
	}

	private static int assertPositive(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException(name + "=" +  v + " must be > 0");
	}
}
