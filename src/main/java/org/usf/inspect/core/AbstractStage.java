package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractStage implements Metric {

	private final String requestId;
	private final int order; // stages has same start sometimes (duration=0)

	private String name; // rename to type
	private Instant start;
	private Instant end;
	private ExceptionInfo exception;
	private String command;
//	private String threadName
	
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