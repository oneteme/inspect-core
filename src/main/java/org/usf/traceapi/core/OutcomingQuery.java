package org.usf.traceapi.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;

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
public class OutcomingQuery implements Metric {

	private String host;
	private Integer port; //nullable
	private String schema; //nullable
	private Instant start;
	private Instant end;
	private String user;
	private String threadName;
	private String driverVersion;
	private String databaseName;
	private String databaseVersion;
	private boolean completed = true; // initial status
	private final List<DatabaseAction> actions = new LinkedList<>();
	
	public void append(DatabaseAction action) {
		actions.add(action);
		completed &= isNull(action.getException());
	}
	
	@Override
	public String toString() {
		return format("%-20s", threadName) + ": QUERY   {" +  format("%5s", duration()) + "ms}"; 
	}
}
