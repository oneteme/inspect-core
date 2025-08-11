package org.usf.inspect.core;

import java.time.Instant;

import org.usf.inspect.jdbc.JDBCAction;

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
	
	public DatabaseRequestStage createStage(JDBCAction type, Instant start, Instant end, Throwable t, long[] count) {
		var stg = createStage(type, start, end, t, DatabaseRequestStage::new);
		stg.setCount(count);
		return stg;
	}

	@Override
	public DatabaseRequest copy() {
		return new DatabaseRequest(this);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withCommand(command)
		.withUser(getUser())
		.withUrlAsResource("jdbc:"+productName.toLowerCase(), host, port, name, null)
		.withPeriod(getStart(), getEnd())
		.format();
	}
}
