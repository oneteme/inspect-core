package org.usf.inspect.core;

import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

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
	private byte traceType; //TraceType
	private long offset; //order | duration
	private UUID traceId; //request | session
	
	public static ExceptionTrace fromException(Throwable thrw) {
		return fromException(thrw, 0, 0);
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
				.withInstant(traceType < 10 ? ofEpochMilli(offset) : null)
//				.withAction(command)
				.withAction(type)
				.withMessageAsTopic(message)
				.format();
	}
}
