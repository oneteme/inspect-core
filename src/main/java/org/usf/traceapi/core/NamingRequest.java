package org.usf.traceapi.core;

import static org.usf.traceapi.core.Helper.prettyURLFormat;

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
public class NamingRequest extends SessionStage {
	
	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	private List<NamingRequestStage> actions;

	@Override
	String prettyFormat() {
		return prettyURLFormat(getUser(), protocol, host, port, null);
	}
}
