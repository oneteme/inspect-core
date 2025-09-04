package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class MailRequestStage extends AbstractStage {
	
	private Mail mail;

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(getName())
		.withArgsAsTopic(getCommand(), nonNull(mail) ? new Object[] {mail.getSubject()} : null)
		.withPeriod(getStart(), getEnd())
		.withResult(getException())
		.format();
	}
}