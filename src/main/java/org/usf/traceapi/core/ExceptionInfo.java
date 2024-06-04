package org.usf.traceapi.core;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ExceptionInfo {
	
	private final String classname; //type
	private final String message;
	//stack
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage());
		}
		return null;
	}
}
