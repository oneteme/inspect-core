package org.usf.inspect.core;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class SessionAsyncDurationUpdate implements TracePart {

	private final UUID id;
	private final boolean main;
	private final long asyncDuration;
	
	
	@Override
	public String toString() {
		return id + "{set asyncDuration=" + asyncDuration + "}";
	}
}
