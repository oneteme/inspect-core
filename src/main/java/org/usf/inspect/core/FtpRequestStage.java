package org.usf.inspect.core;

import java.util.UUID;

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

	@Deprecated(since = "1.2", forRemoval = true)
	private String[] args;

	public FtpRequestStage(UUID requestId, int order) {
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