package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DirectoryRequestSignal extends AbstractRequestSignal {

	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	
	public DirectoryRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public DirectoryRequestUpdate createCallback() {
		return new DirectoryRequestUpdate(getId());
	}
}
