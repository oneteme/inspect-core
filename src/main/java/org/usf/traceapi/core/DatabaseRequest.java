package org.usf.traceapi.core;

import static java.util.Objects.isNull;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonIgnoreProperties("exception")
public class DatabaseRequest extends RunnableStage {

	private String host;
	private Integer port; //nullable
	private String schema; //nullable
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
	public ExceptionInfo getException() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setException(ExceptionInfo exception) {
		throw new UnsupportedOperationException();
	}
	
}
