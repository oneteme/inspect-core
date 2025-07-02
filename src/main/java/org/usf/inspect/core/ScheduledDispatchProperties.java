package org.usf.inspect.core;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.DispatchState.DISPTACH;

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
	private int bufferSize = 100; // {n} sessions
	private int bufferMaxSize = 5_000; // {n} sessions, -1: unlimited
	private DispatchState state = DISPTACH;

	void validate() {
		assertPositive(delay, "delay");
		assertPositive(bufferSize, "bufferSize");
		requireNonNull(state, "state cannot be null");
	}

	private static int assertPositive(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException(name + "=" +  v + " must be > 0");
	}
}
