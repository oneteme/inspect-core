package org.usf.inspect.core;

import static org.usf.inspect.core.Trace.Level.CORE;
import static org.usf.inspect.core.Trace.Level.ERROR;
import static org.usf.inspect.core.Trace.Level.INFO;
import static org.usf.inspect.core.Trace.Level.WARN;

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

	
	public static Trace info(String msg) {
		return trace(INFO, msg);
	}

	public static Trace  warn(String msg) {
		return trace(WARN, msg);
	}
	
	public static Trace error(String msg) {
		return trace(ERROR, msg);
	}
	
	 static Trace core(String msg) {
		return trace(CORE, msg);
	}
	
	static Trace trace(Level level, String msg) {
		return new Trace(Instant.now(), level, msg);
	}
}