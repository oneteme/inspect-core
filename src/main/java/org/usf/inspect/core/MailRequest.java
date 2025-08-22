package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.List;

import org.usf.inspect.mail.MailAction;

import com.fasterxml.jackson.annotation.JsonCreator;

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

	@JsonCreator public MailRequest() { }

	MailRequest(MailRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.mails = req.mails;
		this.failed = req.failed;
	}
	
	public MailRequestStage createStage(MailAction type, Instant start, Instant end, Throwable thrw) {
		return createStage(type, start, end, thrw, MailRequestStage::new);
	}

	@Override
	public MailRequest copy() {
		return new MailRequest(this);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withCommand("SEND")
		.withUser(getUser())
		.withUrlAsTopic(protocol, host, port, null, null)
		.withPeriod(getStart(), getEnd())
		.withResult(nonNull(mails) ? mails.size() + "mails" : null)
		.format();
	}
	
	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}
