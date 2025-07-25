package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.appendStackTrace;
import static org.usf.inspect.core.StackTraceRow.excetionStackTraceRows;
import static org.usf.inspect.core.StackTraceRow.threadStackTraceRows;

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
	private final Level level;
	private final String message;
	private final StackTraceRow[] stackRows;
	private String sessionId; //nullable
	
	@Override
	public String toString() {
		var sb = new StringBuilder(instant + " [" + level + "] " + message);
		appendStackTrace(sb, stackRows);
		return sb.toString();
	}
	
	public static LogEntry logEntry(Level lvl, String msg) {
		return logEntry(lvl, msg, null, 0); // 0 => no stack	
	}
	
	public static LogEntry logEntry(Level lvl, String msg, int maxStack) {
		return logEntry(lvl, msg, null, maxStack);	
	}
	
	public static LogEntry logEntry(Level lvl, String msg, Throwable e, int maxStack) {
		return new LogEntry(now(), lvl, msg, nonNull(e) 
				? excetionStackTraceRows(e, maxStack) 
				: threadStackTraceRows(maxStack));	
	}

	public enum Level {
		INFO, WARN, ERROR;
	}
}