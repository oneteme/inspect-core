package org.usf.traceapi.core;

import static java.time.Duration.between;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class OutcomingQuery {

	private Instant start;
	private Instant end;
	private final List<DatabaseAction> actions;
	
	public OutcomingQuery() {
		this.actions = new LinkedList<>();
	}
	
	public void append(DatabaseAction query) {
		actions.add(query);
	}
	
	@Override
	public String toString() {
		return "QUERY {" + between(start, end).toMillis() + "ms}";
	}
}
