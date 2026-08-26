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
public class DirectoryRequestStage extends AbstractStage {

	@Deprecated(since = "1.2", forRemoval = true)
	private String[] args;
	
	public DirectoryRequestStage(UUID requestId, int order) {
		super(requestId, order);
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(getName())
		.withArgsAsTopic(getCommand(), args)
		.withPeriod(getStart(), getEnd())
		.withResult(nonNull(args) ? Arrays.toString(args) : getException())
		.format();
	}	
}
