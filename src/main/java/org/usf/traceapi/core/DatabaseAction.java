package org.usf.traceapi.core;

import static java.time.Duration.between;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@RequiredArgsConstructor
public class DatabaseAction {

	private final Action type;
	private final Instant start;
	private final Instant end;
	private final boolean failed;
	//private final long count;
	
	@Override
	public String toString() {
		return type + " {" + between(start, end).toMillis() + "ms}";
	}
}