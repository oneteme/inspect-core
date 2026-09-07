package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractStage implements Metric {

	private UUID requestId;
	private long order; // stages has same start sometimes (duration=0)

	private String name; // rename to type
	private Instant start;
	private Instant end;
	private String command;
	@Deprecated(forRemoval = true, since = "v1.2")
	private ExceptionTrace exception;
	//v1.2
	private StagePayload payload;
//	private String threadName
	
	AbstractStage(UUID requestId, long order) {
		this.requestId = requestId;
		this.order = order;
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withAction(name)
				.withArgsAsTopic(command, nonNull(payload) && nonNull(payload.getArgs()) ? payload.getArgs() : null)
				.withPeriod(start, end)
				.withResult(nonNull(payload) && nonNull(payload.getCount()) ? Arrays.toString(payload.getCount()) : null)
				.format();
	}
}