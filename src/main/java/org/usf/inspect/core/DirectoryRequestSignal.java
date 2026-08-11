package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Signal that describes the start of a directory request.
 */
@Getter
@Setter
public final class DirectoryRequestSignal extends AbstractRequestSignal {

	private String protocol; // ldap, ldaps
	private String host;  //IP, domain
	private int port; // positive number, -1 otherwise
	
	/**
	 * Creates a directory request signal.
	 *
	 * @param id the request identifier
	 * @param sessionId the owning session identifier
	 * @param start the request start time
	 * @param threadName the originating thread name
	 */
	public DirectoryRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	/**
	 * Creates the mutable update associated with this request.
	 *
	 * @return the directory request update
	 */
	public DirectoryRequestUpdate createCallback() {
		return new DirectoryRequestUpdate(getId());
	}
}
