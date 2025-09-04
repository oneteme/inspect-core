package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.Arrays;

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

	private long[] count;
	private String[] args; // only for BATCH|EXECUTE|FETCH
		
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