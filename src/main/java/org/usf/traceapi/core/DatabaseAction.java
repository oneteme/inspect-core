package org.usf.traceapi.core;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@RequiredArgsConstructor
public class DatabaseAction implements Metric {

	private final Action type;
	private final Instant start;
	private final Instant end;
	private final boolean failed;
	//private final long rows;
	
	@Override
	public String toString() {
		return type + " {" + duration() + "ms}";
	}
}