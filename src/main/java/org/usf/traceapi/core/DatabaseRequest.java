package org.usf.traceapi.core;

import static java.util.Objects.isNull;

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
	
	@Deprecated(forRemoval = true, since = "17")
	public String getSchema(){
		return database;
	}

	@Deprecated(forRemoval = true, since = "17")
	public void setSchema(String schema) {
		this.database = schema;
	}
	
}
