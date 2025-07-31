package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.Arrays;

import org.usf.inspect.jdbc.SqlCommand;

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

	private long[] count; // only for BATCH|EXECUTE|FETCH
	private SqlCommand[] commands;
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withCommand(getName())
		.withArgsAsResource(commands)
		.withPeriod(getStart(), getEnd())
		.withResult(nonNull(count) ? Arrays.toString(count) : getException())
		.format();
	}
}