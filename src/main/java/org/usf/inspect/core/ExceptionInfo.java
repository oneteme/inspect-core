package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.fromStackTrace;

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
public class ExceptionInfo {
	
	private final String type;
	private final String message;
	private final StackTraceRow[] stack;

	@Override
	public String toString() {
		var sb = new StringBuilder(type + ": " + message);
		if(nonNull(stack)) {
			for(var row : stack) {
				sb.append("\n  at ").append(row);
			}
		}
		return sb.toString();
	}
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage(), null);
		}
		return null;
	}
	
	public static ExceptionInfo from(Throwable thrw, int maxStack) {
		if(nonNull(thrw)) {
			var stack = maxStack > 0 ? fromStackTrace(thrw.getStackTrace(), maxStack) : null;
			return new ExceptionInfo(
					thrw.getClass().getName(), 
					thrw.getMessage(), 
					stack);
		}
		return null;
	}
}
