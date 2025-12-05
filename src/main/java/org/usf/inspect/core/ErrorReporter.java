package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.REPORT;
import static org.usf.inspect.core.StackTraceRow.excetionStackTraceRows;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorReporter {

	public static void reportError(boolean stack, String action, Throwable thwr) {
		report(stack, format(action, null, thwr), thwr);
	}

	public static void reportMessage(boolean stack, String action, String msg) {
		report(stack, format(action, msg, null), null);
	}

	static String format(String action, String msg, Throwable thwr) {
		var sb = new StringBuilder();
		sb.append("thread=").append(threadName());
		if(nonNull(action)) {
			sb.append(", action=").append(action);
		}
		if(nonNull(msg)) {
			sb.append(", message=").append(msg);
		}
		if(nonNull(thwr)) {
			sb.append(", cause=").append(thwr.getClass().getName())
			.append(":").append(thwr.getMessage());
		}
		return sb.toString();
	}

	static void report(boolean stack, String msg, Throwable cause) {
		StackTraceRow[] arr = null;
		if(stack && context().getConfiguration().isDebugMode()) {
			arr = excetionStackTraceRows(requireNonNullElseGet(cause, Exception::new), -1);
		}
		logEntry(REPORT, msg, arr).emit();
	}
	
	public static boolean assertMonitorNonNull(Object monitor, String action) {
		if (isNull(monitor)) {
			reportMessage(false, action, "monitor is null");
			return false;
		}
		return true;
	}
}
