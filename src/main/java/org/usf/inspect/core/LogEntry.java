package org.usf.inspect.core;

import static java.time.Clock.systemUTC;

import java.time.Instant;
import java.util.UUID;

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
	@Deprecated(forRemoval = true, since = "v1.2")
	private final Level level; //type
	private final String message;
	private final StackTraceRow[] stackRows;
	private UUID sessionId; //optional
	
	//server usage 
	private UUID instanceId; 
	
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

	public static enum Level {
		INFO, WARN, ERROR, REPORT;
	}
}