package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class ExceptionInfo {
	
	private final String type; //className
	private final String message;
	//v1.1
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionInfo cause; //optional, can be null
	
	@Override
	public String toString() {
		return type + ": " + message;
	}
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage(), null, null);
		}
		return null;
	}

	public static Throwable rootCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return t;
		}
		//si t déja null au début ce return va envoyer  null
		return t;

	}

	public static ExceptionInfo fromException(Throwable thrw) {
		var config = hub().getConfiguration().getMonitoring().getException();
		return fromException(thrw, config.getMaxCauseDepth(), config.getMaxStackTraceRows());
	}
	
	public static ExceptionInfo fromException2(Throwable thrw) { //TODO rename
		return fromException(thrw, 0, 0);
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
