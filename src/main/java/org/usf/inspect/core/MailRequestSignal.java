package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the signal emitted when a mail request starts executing.
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

	/**
	 * Creates a mail request signal with the supplied request and execution context.
	 *
	 * @param id the request identifier
	 * @param sessionId the owning session identifier
	 * @param start the request start time
	 * @param threadName the thread that started the request
	 */
	public MailRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	/**
	 * Creates the update object that collects the outcome of this mail request.
	 *
	 * @return the update associated with this signal
	 */
	public MailRequestUpdate createCallback() {
		return new MailRequestUpdate(getId());
	}
}
