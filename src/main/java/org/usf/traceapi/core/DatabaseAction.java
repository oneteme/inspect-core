package org.usf.traceapi.core;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public final class DatabaseAction implements Metric {

	private final SqlAction type;
	private final Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	private long count;
	
	@Override
	public String toString() {
		return type + " {" + duration() + "ms}";
	}
}