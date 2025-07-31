package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.List;

import org.usf.inspect.mail.MailAction;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public class MailRequest extends AbstractRequest {

	private String protocol; //smtp(s), imap, pop3
	private String host;
	private int port;
	private List<Mail> mails;
	//v1.1
	private boolean failed;
	//mail-collector
	
	public MailRequestStage createStage(MailAction type, Instant start, Instant end, Throwable thrw) {
		return createStage(type, start, end, thrw, MailRequestStage::new);
	}

	@Override
	public MailRequest copy() {
		var req = new MailRequest();
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setProtocol(protocol);
		req.setHost(host);
		req.setPort(port);
		req.setMails(mails);
		req.setFailed(failed);
		return req;
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withCommand("SEND")
		.withUser(getUser())
		.withUrlAsResource(protocol, host, port, null, null)
		.withPeriod(getStart(), getEnd())
		.withResult(nonNull(mails) ? mails.size() + "mails" : null)
		.format();
	}
}
