package org.usf.inspect.core;

import java.time.Instant;

import org.usf.inspect.dir.DirAction;

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
public class DirectoryRequest extends AbstractRequest {
	
	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	//v1.1
	private boolean failed;

	@JsonCreator public DirectoryRequest() { }

	DirectoryRequest(DirectoryRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.failed = req.failed;
	}
	
	public DirectoryRequestStage createStage(DirAction type, Instant start, Instant end, Throwable t, String... args) {
		var stg = createStage(type, start, end, t, DirectoryRequestStage::new);
		stg.setArgs(args);
		return stg;
	}

	@Override
	public DirectoryRequest copy() {
		return new DirectoryRequest(this);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withUser(getUser())
		.withUrlAsResource(protocol, host, port, null, null)
		.withPeriod(getStart(), getEnd())
		.format();
	}
}
