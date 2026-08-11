package org.usf.inspect.core;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.nonNull;

import java.time.Instant;

/**
 * Defines a time-based event trace that exposes start, end, and duration information.
 *
 * @author u$f
 *
 */
public interface Metric extends EventTrace {

	Instant getStart();

	Instant getEnd();
	
	/**
	 * Returns the duration of the metric in milliseconds when both endpoints are available.
	 *
	 * @return the metric duration in milliseconds, or {@code -1} when the duration is not yet known
	 */
	default long duration(){
		return nonNull(getStart()) && nonNull(getEnd()) 
				? getStart().until(getEnd(), MILLIS)
				: -1; // not set yet
	}
}
