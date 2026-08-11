package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

/**
 * Stage information for a directory request operation.
 */
@Getter
@Setter
public class DirectoryRequestStage extends AbstractStage {
	
	private String[] args;
	//int count !?
	/**
	 * Returns a formatted string representation of this directory stage.
	 *
	 * @return the formatted stage description
	 */
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
