package org.usf.traceapi.core;

import static java.lang.String.format;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class OutcomingQuery implements Metric {

	private String host;
	private Integer port; //nullable
	private String schema; //nullable
	private Instant start;
	private Instant end;
	private String databaseName;
	private String databaseVersion;
	private String driverVersion;
	private String thread;
	private boolean failed;
	private final List<DatabaseAction> actions;
	
	@JsonCreator //remove this
	public OutcomingQuery() {
		this.actions = new LinkedList<>();
	}
	
	public void append(DatabaseAction action) {
		actions.add(action);
		failed &= action.isFailed();
	}
	
	@Override
	public String toString() {
		return format("%-20s", thread) + ": QUERY   {" +  format("%5s", duration()) + "ms}"; 
	}
}
