package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

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
@Getter
@Setter
@RequiredArgsConstructor
public final class ExceptionTrace implements EventTrace {
	
	private final String type; //className
	private final String message;
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionTrace cause; //optional, can be null
	//v1.2
	private UUID traceId; //request | session
	private long offset; //order | duration
	
	public static ExceptionTrace fromException(Throwable thrw) {
		var config = hub().getConfiguration().getMonitoring().getException();
		return fromException(thrw, config.getMaxCauseDepth(), config.getMaxStackTraceRows());
	}
	
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
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(offset < 0 ? Instant.ofEpochMilli(offset) : null)
//				.withAction(command)
				.withAction(type)
				.withMessageAsTopic(message)
				.format();
	}
}
