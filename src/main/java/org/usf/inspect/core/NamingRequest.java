package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import java.time.Instant;

import org.usf.inspect.naming.NamingAction;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class NamingRequest extends AbstractRequest {
	
	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	//v1.1
	private boolean failed;
	
	public NamingRequestStage createStage(NamingAction type, Instant start, Instant end, Throwable t, String... args) {
		var stg = createStage(type, start, end, t, NamingRequestStage::new);
		stg.setArgs(args);
		return stg;
	}

	@Override
	public NamingRequest copy() {
		var req = new NamingRequest();
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
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
