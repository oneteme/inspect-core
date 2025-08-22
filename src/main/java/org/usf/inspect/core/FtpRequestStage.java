package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class FtpRequestStage extends AbstractStage {

	private String[] args;

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withCommand(getName())
		.withArgsAsTopic(args)
		.withPeriod(getStart(), getEnd())
		.withResult(getException())
		.format();
	}
}