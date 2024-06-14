package org.usf.traceapi.core;

import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ExceptionInfo {
	
	private final String type;
	private final String message;
	//stack

	@Deprecated(forRemoval = true, since = "v22")
	public String getClassname() {
		return type;
	}
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage());
		}
		return null;
	}
	
	@Override
	public String toString() {
		return type + ": " + message;
	}
}
