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
public final class MailRequestSignal extends AbstractRequestSignal {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;

	public MailRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public MailRequestUpdate createCallback() {
		return new MailRequestUpdate(getId());
	}
}
