package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

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
	
	public DatabaseRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withUrlAsTopic(scheme, host, port, schema, null)
				.withUser(getUser())
				.format();
	}
}
