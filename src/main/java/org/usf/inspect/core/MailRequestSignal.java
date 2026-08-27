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
public final class MailRequestSignal extends AbstractRequestSignal {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;

	public MailRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	@Deprecated
	public MailRequestUpdate createCallback() {
		return new MailRequestUpdate(getId());
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
