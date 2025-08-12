package org.usf.inspect.core;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static org.usf.inspect.core.Assertions.assertBetween;

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

	void validate() {
		assertBetween(interval, ofSeconds(10), ofHours(1), "interval");
	}
}