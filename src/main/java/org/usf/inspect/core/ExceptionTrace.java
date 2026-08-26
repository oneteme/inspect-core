package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class ExceptionTrace implements EventTrace {
	
	private final String type; //className
	private final String message;
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionTrace cause; //optional, can be null
	//v1.2
	private final UUID traceId; //request | session
	private final int offset; //order | duration
	
	@Override
	public String toString() {
		return type + ": " + message;
	}
	
	public static ExceptionTrace fromException(Throwable thrw) {
		var config = hub().getConfiguration().getMonitoring().getException();
		return fromException(thrw, config.getMaxCauseDepth(), config.getMaxStackTraceRows());
	}
	
	@Deprecated //TODO set traceId & offset
	public static ExceptionTrace fromException(Throwable thrw, int maxCauses, int maxRows) {
		if(nonNull(thrw)) {
			var cause = thrw.getCause();
			return new ExceptionTrace(
					thrw.getClass().getName(), 
					thrw.getMessage(), 
					exceptionStackTraceRows(thrw, maxRows),
					maxCauses != 0 && nonNull(cause) && thrw != cause ? fromException(cause, --maxCauses, maxRows) : null);
		}
		return null;
	}
}
