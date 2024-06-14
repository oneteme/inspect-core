package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static org.usf.traceapi.core.Helper.prettyFormat;

import java.util.List;

import org.usf.traceapi.jdbc.SqlCommand;

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
public class DatabaseRequest extends SessionStage {

	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String database; //nullable
	private String driverVersion;
	private String databaseName;
	private String databaseVersion;
	private List<DatabaseRequestStage> actions;
	private List<SqlCommand> commands;
	//java-collector
	
	public boolean isCompleted() {
		return actions.stream().allMatch(a-> isNull(a.getException()));
	}
	
	@Override
	public ExceptionInfo getException() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setException(ExceptionInfo exception) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return '['+databaseName+"] "+ prettyFormat(getUser(), "JDBC", host, port, database);
	}
}
