package org.usf.traceapi.core;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@AllArgsConstructor
public final class DatabaseAction implements Metric {

	private final JDBCAction type;
	private final Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	private Integer count;
	
	@Override
	public String toString() {
		return type + " {" + duration() + "ms}";
	}
}