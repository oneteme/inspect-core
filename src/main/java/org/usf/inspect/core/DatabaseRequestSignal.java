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
public final class DatabaseRequestSignal extends AbstractRequestSignal {

	private String scheme;
	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	
	public DatabaseRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public DatabaseRequestUpdate createCallback() {
		return new DatabaseRequestUpdate(getId());
	}
}
