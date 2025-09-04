package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

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
	//v1.1
	private boolean failed;
	//mail-collector

	@JsonCreator public MailRequest() { }

	MailRequest(MailRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.failed = req.failed;
	}
	
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, MailCommand cmd, Throwable thrw) {
		runSynchronized(()->{ 
			if(nonNull(cmd)) {
				setCommand(merge(getCommand(), cmd.getType()));
			}
			if(nonNull(thrw)) {
				failed = true; 
			}
		});
		return createStage(action, start, end, cmd, thrw, MailRequestStage::new);
	}
	
	@Override
	public MailRequest copy() {
		return new MailRequest(this);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withAction(getCommand())
		.withUser(getUser())
		.withUrlAsTopic(protocol, host, port, null, null)
		.withPeriod(getStart(), getEnd())
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
