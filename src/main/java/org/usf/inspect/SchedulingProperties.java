package org.usf.inspect;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.Assertions.assertBetween;
import static org.usf.inspect.core.Assertions.assertGreaterOrEquals;
import static org.usf.inspect.core.DispatchState.DISPATCH;

import java.time.Duration;

import org.usf.inspect.core.DispatchState;

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
//TODO dispatching
public class SchedulingProperties {
	
	private Duration interval = ofSeconds(60);
	private DispatchState state = DISPATCH;
	//v1.2
	private String threadName = "inspect-agt-publisher";
	private int queueCapacity = 100_000; // {n} max buffering traces, min=100

	public void validate() {
		assertBetween(interval, ofSeconds(10), ofHours(1), "interval");
		assertGreaterOrEquals(queueCapacity, 100, "queue-capacity");
		if(isNull(threadName) || !threadName.matches("[\\w-]+")) {
			throw new IllegalArgumentException("name='%s' is not a valid identifier [\\w-]+".formatted(threadName));
		}
	}
}