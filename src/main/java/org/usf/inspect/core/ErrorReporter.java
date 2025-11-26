package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorReporter {

	private final boolean stack;
	private String action;
	private String message;
	private String thread;
	private Throwable cause;

	public ErrorReporter action(String action) {
		this.action = action;
		return this;
	}

	public ErrorReporter message(String message) {
		this.message = message;
		return this;
	}

	public ErrorReporter thread() {
		this.thread = threadName();
		return this;
	}


	public ErrorReporter cause(Throwable cause) {
		this.cause = cause;
		return this;
	}

	public void emit() {
		var stk = stack || context().getConfiguration().isDebugMode() ? -1 : 0;
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
		if(nonNull(thread)) {
			sb.append(", thread=").append(thread);
		}
		if(nonNull(cause)) {
			sb.append(", cause=").append(cause.getClass().getSimpleName())
			.append(": ").append(cause.getMessage());
		}
		return sb.toString();
	}

	public static ErrorReporter stackReporter(){
		return new ErrorReporter(true).thread();
	}

	public static void reportError(String action, Throwable cause) {
		new ErrorReporter(false).action(action).cause(cause).thread().emit();
	}

	public static void reportMessage(String action, String message) {
		new ErrorReporter(false).action(action).message(message).thread().emit();
	}
}
