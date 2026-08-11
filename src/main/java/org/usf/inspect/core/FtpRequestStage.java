package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;

/**
 * Stage information for an FTP request operation.
 */
@Getter
@Setter
public final class FtpRequestStage extends AbstractStage {

	private String[] args;

	/**
	 * Returns a formatted string representation of this FTP stage.
	 *
	 * @return the formatted stage description
	 */
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