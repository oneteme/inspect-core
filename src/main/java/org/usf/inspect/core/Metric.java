package org.usf.inspect.core;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Metric extends EventTrace {
	
	Instant getStart();

	Instant getEnd();
	
	default long duration(){
		return nonNull(getStart()) && nonNull(getEnd()) 
				? getStart().until(getEnd(), MILLIS)
				: -1; // not set yet
	}

	static String prettyDurationFormat(Metric m) {
		return "(in " + m.duration() + "ms)";
	}
}
