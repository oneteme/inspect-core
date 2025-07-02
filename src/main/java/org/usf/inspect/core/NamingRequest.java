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
	
	@Override
	protected NamingRequestStage createStage() {
		return new NamingRequestStage();
	}
	
	@Override
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
