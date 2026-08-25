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

	public FtpRequestStage(String requestId, int order) {
		super(requestId, order);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(getName())
		.withArgsAsTopic(getCommand(), args)
		.withPeriod(getStart(), getEnd())
		.withResult(getException())
		.format();
	}
}