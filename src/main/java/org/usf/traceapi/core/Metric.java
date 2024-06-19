package org.usf.traceapi.core;

import static java.time.Duration.between;
import static java.util.Objects.isNull;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Metric {
	
	Instant getStart();

	Instant getEnd();
	
	default long duration(){
		return isNull(getStart()) || isNull(getEnd()) 
				? -1 // not set yet
				: between(getStart(), getEnd()).toMillis();
	}

	static String prettyDurationFormat(Metric m) {
		return " (in " + m.duration() + "ms)";
	}
}
