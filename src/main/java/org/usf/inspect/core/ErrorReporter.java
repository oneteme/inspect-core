package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorReporter {

	private final boolean stack;
	private String action;
	private String message;
	private EventTrace trace;
	private Throwable cause;

	public ErrorReporter action(String action) {
		this.action = action;
		return this;
	}

	public ErrorReporter message(String message) {
		this.message = message;
		return this;
	}

	public ErrorReporter trace(EventTrace trace) {
		this.trace = trace;
		return this;
	}

	public ErrorReporter cause(Throwable cause) {
		this.cause = cause;
		return this;
	}

	public void emit() {
		var stk = stack && context().getConfiguration().isDebugMode() ? -1 : 0;
		logEntry(ERROR, toString(), stk).emit();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		if(nonNull(action)) {
			sb.append("action=").append(action);
		}
		if(nonNull(message)) {
			sb.append(", message=").append(message);
		}
		if(nonNull(trace)) {
			sb.append(", trace=").append(trace);
		}
		if(nonNull(cause)) {
			sb.append(", cause=").append(cause.getClass().getSimpleName())
			.append(": ").append(cause.getMessage());
		}
		return sb.toString();
	}

	public static ErrorReporter reporter(){
		return new ErrorReporter(true);
	}

	public static void reportError(String action, EventTrace trace, Throwable cause) {
		new ErrorReporter(false).action(action).trace(trace).cause(cause).emit();
	}

	public static void reportMessage(String action, EventTrace trace, String message) {
		new ErrorReporter(false).action(action).trace(trace).message(message).emit();
	}
}
