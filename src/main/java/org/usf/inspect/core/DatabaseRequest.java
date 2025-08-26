package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.jdbc.SqlCommand.SQL;

import java.time.Instant;

import org.usf.inspect.jdbc.JDBCAction;
import org.usf.inspect.jdbc.SqlCommand;

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
public class DatabaseRequest extends AbstractRequest {

	private String scheme;
	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	//v1.1
	private boolean failed;
	private String command;
	//java-collector

	@JsonCreator public DatabaseRequest() { }

	DatabaseRequest(DatabaseRequest req) {
		super(req);
		this.scheme = req.scheme;
		this.host = req.host;
		this.port = req.port;
		this.name = req.name;
		this.schema = req.schema;
		this.driverVersion = req.driverVersion;
		this.productName = req.productName;
		this.productVersion = req.productVersion;
		this.failed = req.failed;
		this.command = req.command;
	}
	
	public DatabaseRequestStage createStage(JDBCAction type, Instant start, Instant end, Throwable thrw, long[] count) {
		if(nonNull(thrw)) {
			runSynchronized(()-> failed = true);
		}
		var stg = createStage(type, start, end, thrw, DatabaseRequestStage::new);
		stg.setCount(count);
		return stg;
	}
	
	public void appendCommand(SqlCommand cmd) {
		if(isNull(command)) {
			command = isNull(cmd) ? "?" : cmd.name();
		}
		else if(isNull(cmd) || !command.equals(cmd.name())){
			command = SQL.name(); //multiple
		}
	}

	@Override
	public DatabaseRequest copy() {
		return new DatabaseRequest(this);
	}
	
	@Override
	public String toString() {
		var prod = nonNull(productName) ? productName.toLowerCase() : null;
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withCommand(command)
		.withUser(getUser())
		.withUrlAsTopic("jdbc:"+prod, host, port, name, null)
		.withPeriod(getStart(), getEnd())
		.format();
	}
	
	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}
