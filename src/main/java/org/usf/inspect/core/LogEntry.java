package org.usf.inspect.core;

import static java.time.Clock.systemUTC;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a log event captured by the inspection core together with its severity and stack context.
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@RequiredArgsConstructor
public final class LogEntry implements EventTrace {
	
	private final Instant instant;
	private final Level level;
	private final String message;
	private final StackTraceRow[] stackRows;
	private String sessionId; //nullable
	private String instanceId; //server usage 
	
	/**
	 * Returns this log entry as a formatted event trace string.
	 *
	 * @return the formatted string representation of this log entry
	 */
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(level.name())
		.withMessageAsTopic(message)
		.withInstant(instant)
		.format();
	}
	
	static LogEntry logEntry(Level lvl, String msg) {
		return logEntry(lvl, msg, null);	
	}
	
	static LogEntry logEntry(Level lvl, String msg, StackTraceRow[] stack) {
		return new LogEntry(systemUTC().instant(), lvl, msg, stack);	
	}

	/**
	 * Defines the supported severity levels for log entries.
	 */
	public enum Level {
		INFO, WARN, ERROR, REPORT;
	}
}