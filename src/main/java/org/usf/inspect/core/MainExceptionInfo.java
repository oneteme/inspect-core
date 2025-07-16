package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.StackTraceRow.fromStackTrace;

import java.util.ArrayList;

import lombok.Getter;

@Getter
public class MainExceptionInfo extends ExceptionInfo {

	private final ExceptionInfo[] causes;

	public MainExceptionInfo(String type, String message, StackTraceRow[] stack, ExceptionInfo[] causes) {
		super(type, message, stack);
		this.causes = causes;
	}
	
	public static MainExceptionInfo form(Throwable thrw, int maxStack) {
		if(nonNull(thrw)) {
			var stack = maxStack > 0 ? fromStackTrace(thrw.getStackTrace(), maxStack) : null;
			return new MainExceptionInfo(
					thrw.getClass().getName(), 
					thrw.getMessage(), 
					stack,
					getCauses(thrw, maxStack));
		}
		return null;
	}
	
	static ExceptionInfo[] getCauses(Throwable thrw, int maxStack) {
		var causes = new ArrayList<ExceptionInfo>();
		while(nonNull(thrw.getCause()) && thrw != thrw.getCause()) {
			causes.add(ExceptionInfo.from(thrw.getCause(), maxStack));
			thrw = thrw.getCause();
		}
		return causes.toArray(ExceptionInfo[]::new);
	} 

	@Override
	public String toString() {
		var sb = new StringBuilder(super.toString());
		if(nonNull(causes)) {
			for(var cause : causes) {
				sb.append("\nCaused by: ").append(cause);
			}
		}
		return sb.toString();
	}
}
