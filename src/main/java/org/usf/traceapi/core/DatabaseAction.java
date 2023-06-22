package org.usf.traceapi.core;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class DatabaseAction implements Metric {

	private final Action type;
	private final Instant start;
	private final Instant end;
	private final boolean failed;
	//private final long rows; Statment|Resultset|Update|Batch
	
	@Override
	public String toString() {
		return type + " {" + duration() + "ms}";
	}
}