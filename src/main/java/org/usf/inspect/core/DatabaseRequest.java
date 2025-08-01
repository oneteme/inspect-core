package org.usf.inspect.core;

import java.time.Instant;

import org.usf.inspect.jdbc.JDBCAction;

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
	
	public DatabaseRequestStage createStage(JDBCAction type, Instant start, Instant end, Throwable t, long[] count) {
		var stg = createStage(type, start, end, t, DatabaseRequestStage::new);
		stg.setCount(count);
		return stg;
	}

	@Override
	public DatabaseRequest copy() {
		var req = new DatabaseRequest();
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setScheme(scheme);
		req.setHost(host);
		req.setPort(port);
		req.setName(name);
		req.setSchema(schema);
		req.setDriverVersion(driverVersion);
		req.setProductName(productName);
		req.setProductVersion(productVersion);
		req.setFailed(failed);
		return req;
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
