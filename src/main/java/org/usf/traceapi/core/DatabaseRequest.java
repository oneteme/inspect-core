package org.usf.traceapi.core;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import org.usf.traceapi.jdbc.SqlCommand;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonIgnoreProperties("exception")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
	
	public static DatabaseRequest newDatabaseRequest() {
		var req = new DatabaseRequest();
		req.setActions(new ArrayList<>());
		req.setCommands(new ArrayList<>());
		return req;
	}
}
