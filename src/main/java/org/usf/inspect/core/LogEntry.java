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
	@Deprecated(forRemoval = true, since = "v1.2")
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
	
	@Deprecated(forRemoval = true, since = "v1.2")
	static LogEntry logEntry(Level lvl, String msg) {
		return logEntry(msg, null);	
	}

	@Deprecated(forRemoval = true, since = "v1.2")
	static LogEntry logEntry(String msg, StackTraceRow[] stack) {
		return new LogEntry(systemUTC().instant(), null, msg, stack);	
	}

	public enum Level {
		INFO, WARN, ERROR, REPORT;
	}
}