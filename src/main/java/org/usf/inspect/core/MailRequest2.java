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
public class MailRequest2 extends AbstractRequest2 {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;

	public MailRequest2(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public MailRequestCallback createCallback() {
		return new MailRequestCallback(getId());
	}
}
