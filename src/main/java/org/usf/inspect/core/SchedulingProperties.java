package org.usf.inspect.core;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.Assertions.assertOneOf;
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
	
    private byte delay = 5;
	private TimeUnit unit = SECONDS;

	void validate() {
		assertStrictPositive(delay, "delay");
		assertOneOf(unit, EnumSet.of(SECONDS, MINUTES, HOURS), "unit");
	}
}