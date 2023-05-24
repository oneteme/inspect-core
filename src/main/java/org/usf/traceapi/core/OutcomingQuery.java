package org.usf.traceapi.core;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;

@Getter
public class OutcomingQuery {

	private final List<DatabaseAction> actions;
	
	public OutcomingQuery() {
		this.actions = new LinkedList<>();
	}
}
