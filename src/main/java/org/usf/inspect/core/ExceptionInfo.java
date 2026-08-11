package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Serializable description of an exception and its cause chain.
 */
@Getter
@RequiredArgsConstructor
public final class ExceptionInfo {
	
	private final String type; //className
	private final String message;
	//v1.1
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionInfo cause; //optional, can be null
	
	/**
	 * Returns a concise string representation of this exception information.
	 *
	 * @return the exception type and message
	 */
	@Override
	public String toString() {
		return type + ": " + message;
	}
	
	/**
	 * Creates exception information for the deepest cause of the supplied throwable.
	 *
	 * @param t the throwable to inspect
	 * @return the deepest cause information, or {@code null} when the throwable is {@code null}
	 */
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage(), null, null);
		}
		return null;
	}

	/**
	 * Creates exception information using the active monitoring configuration.
	 *
	 * @param thrw the throwable to convert
	 * @return the created exception information, or {@code null} when the throwable is {@code null}
	 */
	public static ExceptionInfo fromException(Throwable thrw) {
		var config = hub().getConfiguration().getMonitoring().getException();
		return fromException(thrw, config.getMaxCauseDepth(), config.getMaxStackTraceRows());
	}
	
	static ExceptionInfo fromException(Throwable thrw, int maxCauses, int maxRows) {
		if(nonNull(thrw)) {
			var cause = thrw.getCause();
			return new ExceptionInfo(
					thrw.getClass().getName(), 
					thrw.getMessage(), 
					exceptionStackTraceRows(thrw, maxRows),
					maxCauses != 0 && nonNull(cause) && thrw != cause ? fromException(cause, --maxCauses, maxRows) : null);
		}
		return null;
	}
}
