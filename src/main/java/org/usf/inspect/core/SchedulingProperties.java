package org.usf.inspect.core;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.Assertions.assertMatches;
import static org.usf.inspect.core.Assertions.assertOneOf;
import static org.usf.inspect.core.Assertions.assertPositive;
import static org.usf.inspect.core.Assertions.assertStrictPositive;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

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
public class SchedulingProperties {
	
    private int delay = 5;
	private TimeUnit unit = SECONDS;
	private int queueCapacity = 10_000; // {n} max buffering traces, 0: unlimited
	//v1.1
	private int dispatchDelayIfPending = 30; // send pending traces after {n} seconds, 0: send immediately
	private String dumpdirectory = "/tmp"; // dump folder

	void validate() {
		assertStrictPositive(delay, "delay");
		assertOneOf(unit, EnumSet.of(SECONDS, MINUTES, HOURS), "unit");
		assertPositive(queueCapacity, "trace-buffering-size");
		assertPositive(dispatchDelayIfPending, "trace-pending-after");
		assertMatches(dumpdirectory, "(\\/[\\w-]+)+", "dumpDir");
	}
}