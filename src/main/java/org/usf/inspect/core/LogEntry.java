package org.usf.inspect.core;

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

	enum Level {
		INFO, WARN, ERROR;
	}
}