package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.Arrays;
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
public final class DatabaseRequestStage extends AbstractStage {

	@Deprecated(since = "1.2", forRemoval = true)
	private long[] count;
	@Deprecated(since = "1.2", forRemoval = true)
	private String[] args; // only for BATCH|EXECUTE|FETCH

	public DatabaseRequestStage(UUID requestId, int order) {
		super(requestId, order);
	}
		
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(getName())
		.withArgsAsTopic(getCommand(), args)
		.withPeriod(getStart(), getEnd())
		.withResult(nonNull(count) ? Arrays.toString(count) : getException())
		.format();
	}	
}