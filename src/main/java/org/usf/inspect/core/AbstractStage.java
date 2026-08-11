package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Base stage metric for traced request and session operations.
 */
@Getter
@Setter
public abstract class AbstractStage implements Metric {

	private String name; // rename to type
	private Instant start;
	private Instant end;
	private ExceptionInfo exception;
	// v1.1
	private int order; // stages has same start sometimes (duration=0)
	private String command;
	private String requestId;

	/**
	 * Returns a formatted string representation of this stage.
	 *
	 * @return the formatted stage description
	 */
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withAction(name)
				.withArgsAsTopic(command, null)
				.withPeriod(getStart(), getEnd())
				.withResult(exception)
				.format();
	}
}