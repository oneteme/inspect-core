package org.usf.traceapi.core;

import static java.lang.String.format;
import static java.time.Duration.between;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class OutcomingQuery {

	private String url;
	private Instant start;
	private Instant end;
	private String thread;
	private final List<DatabaseAction> actions;
	
	public OutcomingQuery() {
		this.actions = new LinkedList<>();
	}
	
	public void append(DatabaseAction query) {
		actions.add(query);
	}
	
	@Override
	public String toString() {
		return format("%-20s", thread) + ": QUERY   {" +  format("%5s", between(start, end).toMillis()) + "ms}";
	}
}
