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
public class DirectoryRequest2 extends AbstractRequest2 {

	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	
	public DirectoryRequest2(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public DirectoryRequestCallback createCallback() {
		return new DirectoryRequestCallback(getId());
	}
}
