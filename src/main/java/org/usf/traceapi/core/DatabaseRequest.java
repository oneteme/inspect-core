package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static org.usf.traceapi.core.Helper.prettyURLFormat;

import java.util.List;

import org.usf.traceapi.jdbc.SqlCommand;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class DatabaseRequest extends SessionStage {

	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	private List<DatabaseRequestStage> actions;
	private List<SqlCommand> commands;
	//java-collector
	
	public boolean isCompleted() {
		return actions.stream().allMatch(a-> isNull(a.getException()));
	}
	
	@Override
	public String prettyFormat() {
		return '['+productName+']' 
				+ prettyURLFormat(getUser(), "jdbc", host, port, name);
	}
}
