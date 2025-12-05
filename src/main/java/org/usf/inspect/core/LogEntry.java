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
public final class LogEntry implements EventTrace {
	
	private final Instant instant;
	private final Level level; //type
	private final String message;
	private final StackTraceRow[] stackRows;
	private String sessionId; //nullable
	private String instanceId; //server usage 
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(level.name())
		.withMessageAsTopic(message)
		.withInstant(instant)
		.format();
	}
	
	public static LogEntry logEntry(Level lvl, String msg) {
		return logEntry(lvl, msg, null);	
	}
	
	public static LogEntry logEntry(Level lvl, String msg, StackTraceRow[] stack) {
		return new LogEntry(now(), lvl, msg, stack);	
	}

	public enum Level {
		INFO, WARN, ERROR, REPORT;
	}
}