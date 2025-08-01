package org.usf.inspect.core;

import java.time.Instant;

import org.usf.inspect.dir.DirAction;

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
	
	public DirectoryRequestStage createStage(DirAction type, Instant start, Instant end, Throwable t, String... args) {
		var stg = createStage(type, start, end, t, DirectoryRequestStage::new);
		stg.setArgs(args);
		return stg;
	}

	@Override
	public DirectoryRequest copy() {
		var req = new DirectoryRequest();
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setProtocol(protocol);
		req.setHost(host);
		req.setPort(port);
		req.setFailed(failed);
		return req;
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
