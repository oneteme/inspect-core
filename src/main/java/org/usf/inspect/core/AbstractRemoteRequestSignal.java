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
public abstract class AbstractRemoteRequestSignal extends AbstractRequestSignal  {

	private String protocol; //HTTP(S), SMTP(S), (S)FTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	
	AbstractRemoteRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withUrlAsTopic(protocol, host, port, null, null)
				.withUser(getUser())
				.format();
	}

}
