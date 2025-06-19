package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class Trace {
	
	private final Instant instant;
	private final Level level;
	private final String message;
//	private final String[] stack; //class + Line	
	
	enum Level {
		CORE, INFO, WARN, ERROR;
	}
}