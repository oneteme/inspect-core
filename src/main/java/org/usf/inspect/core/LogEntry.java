package org.usf.inspect.core;

import static java.time.Instant.now;

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
final class LogEntry implements EventTrace {
	
	private final Instant instant;
	private final Level level;
	private final String message;
	private String sessionId; //nullable
	
	public static LogEntry log(Level lvl, String msg) {
		return new LogEntry(now(), lvl, msg);	
	}

	public enum Level {
		INFO, WARN, ERROR;
	}
}