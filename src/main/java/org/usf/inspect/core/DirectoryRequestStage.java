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
public class DirectoryRequestStage extends AbstractStage {
	
	private String[] args;
	//int count !?
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
