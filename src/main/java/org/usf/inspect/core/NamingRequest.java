package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.prettyURLFormat;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class NamingRequest extends SessionStage<NamingRequestStage> {
	
	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	private List<NamingRequestStage> actions;

	@Override
	public boolean append(NamingRequestStage action) {
		return actions.add(action);
	}
	
	@Override
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
