package org.usf.traceapi.core;

import static java.time.Duration.between;

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
		return between(getStart(), getEnd()).toMillis();
	}

}
