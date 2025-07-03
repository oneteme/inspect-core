package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class NamingRequest extends AbstractRequest<NamingRequestStage> {
	
	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	//v1.1
	private boolean failed;

	@Override
	public Metric copy() {
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
	protected NamingRequestStage createStage() {
		return new NamingRequestStage();
	}
	
	@Override
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
