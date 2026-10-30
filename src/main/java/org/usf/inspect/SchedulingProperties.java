package org.usf.inspect;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static org.usf.inspect.Assertions.assertBetween;
import static org.usf.inspect.Assertions.assertGreaterOrEquals;
import static org.usf.inspect.DispatchState.DISPATCH;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ToString
//TODO rename to dispatching
public class SchedulingProperties {
	
	private Duration interval = ofSeconds(60);
	private DispatchState state = DISPATCH;
	//v1.2
	private int queueCapacity = 100_000; // {n} max buffering traces, min=100

	public void validate() {
		assertBetween(interval, ofSeconds(10), ofHours(1), "interval");
		assertGreaterOrEquals(queueCapacity, 100, "queue-capacity");
	}
}