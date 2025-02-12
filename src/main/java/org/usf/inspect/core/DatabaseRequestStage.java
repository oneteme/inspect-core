package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.stream.Stream;

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
public final class DatabaseRequestStage extends RequestStage {

	private long[] count; // only for BATCH|EXECUTE|FETCH
	private SqlCommand[] commands;
	
	@Override
	public String prettyFormat() {
		var s = getName();
		if(nonNull(commands)) {
			s += " ~ " + Stream.of(commands)
			.map(c-> nonNull(c) ? c.name() : "?")
			.collect(joining(", "));
		}
		if(nonNull(count)) {// !exception
			s += " >> " + Arrays.toString(count);
		}
		return s;
	}
}