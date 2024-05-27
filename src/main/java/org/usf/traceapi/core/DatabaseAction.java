package org.usf.traceapi.core;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

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
	
	private final JDBCAction type; //rename to name
	private final Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	private long[] count; // only for BATCH|UPDATE|FETCH
	
	@JsonCreator
	public DatabaseAction(JDBCAction type, Instant start, Instant end, ExceptionInfo exception) {
		this(type, start, end, exception, null);
	}
	
	@Override
	public String toString() {
		return type + " {" + duration() + "ms}";
	}
	
}