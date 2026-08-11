package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Signal that describes the start of a database request.
 */
@Getter
@Setter
public final class DatabaseRequestSignal extends AbstractRequestSignal {

	private String scheme;
	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	
	/**
	 * Creates a database request signal.
	 *
	 * @param id the request identifier
	 * @param sessionId the owning session identifier
	 * @param start the request start time
	 * @param threadName the originating thread name
	 */
	public DatabaseRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	/**
	 * Creates the mutable update associated with this request.
	 *
	 * @return the database request update
	 */
	public DatabaseRequestUpdate createCallback() {
		return new DatabaseRequestUpdate(getId());
	}
}
