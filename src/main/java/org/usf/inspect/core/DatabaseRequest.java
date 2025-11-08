package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

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
	}
	
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, long[] count) {
		var stg = createStage(type, start, end, thrw, cmd);
		stg.setCount(count);
		return stg;
	}
		
	public DatabaseRequestStage createStage(DatabaseAction type, Instant start, Instant end, Throwable thrw, DatabaseCommand cmd, String... args) {
		runSynchronized(()->{ 
			if(nonNull(cmd)) {
				setCommand(merge(getCommand(), cmd.getType()));
			}
			if(nonNull(thrw)) {
				failed = true; 
			}
		});
		var stg = createStage(type, start, end, cmd, thrw, DatabaseRequestStage::new);
		stg.setArgs(args);
		return stg;
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
		.withAction(getCommand())
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
