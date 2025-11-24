package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.CommandType.merge;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public class MailRequestCallback extends AbstractRequestCallback {

	private boolean failed;

	public MailRequestCallback(String id) {
		super(id);
	}

	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd, Mail mail) {
		var stg = createStage(action, start, end, thrw, cmd);
		stg.setMail(mail);
		return stg;
	}
	
	public MailRequestStage createStage(MailAction action, Instant start, Instant end, Throwable thrw, MailCommand cmd) {
		if(nonNull(cmd)) {
			setCommand(merge(getCommand(), cmd.getType()));
		}
		if(nonNull(thrw)) {
			failed = true; 
		}
		return createStage(action, start, end, cmd, thrw, MailRequestStage::new);
	}
}
