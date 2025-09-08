package org.usf.inspect.core;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static org.usf.inspect.core.Assertions.assertBetween;
import static org.usf.inspect.core.BasicDispatchState.DISPATCH;

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
public class SchedulingProperties {
	
	private Duration interval = ofSeconds(60);
	//v1.1
	private BasicDispatchState state = DISPATCH;

	void validate() {
		assertBetween(interval, ofSeconds(15), ofHours(1), "interval");
	}
}