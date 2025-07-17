package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.StackTraceRow.appendStackTrace;
import static org.usf.inspect.core.StackTraceRow.excetionStackTraceRows;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class ExceptionInfo {
	
	private final String type; //className
	private final String message;
	//v1.1
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionInfo cause; //optional, can be null
	
	@Override
	public String toString() {
		var sb = new StringBuilder(type + ": " + message);
		if(nonNull(stackTraceRows)) {
			appendStackTrace(sb, stackTraceRows);
		}
		if(nonNull(cause)) {
			sb.append("\nCaused by: ").append(cause);
		}
		return sb.toString();
	}
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage(), null, null);
		}
		return null;
	}

	public static ExceptionInfo fromException(Throwable thrw) {
		var config = context().getConfiguration().getMonitoring().getException();
		return fromException(thrw, config.getMaxCauseDepth(), config.getMaxStackTraceRows());
	}
	
	static ExceptionInfo fromException(Throwable thrw, int maxCauses, int maxRows) {
		if(nonNull(thrw)) {
			var cause = thrw.getCause();
			return new ExceptionInfo(
					thrw.getClass().getName(), 
					thrw.getMessage(), 
					excetionStackTraceRows(thrw, maxRows),
					maxCauses != 0 && nonNull(cause) && thrw != cause ? fromException(cause, --maxCauses, maxRows) : null);
		}
		return null;
	}
}
