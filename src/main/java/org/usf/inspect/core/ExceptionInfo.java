package org.usf.inspect.core;

import static java.util.Objects.nonNull;

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
public final class ExceptionInfo implements Traceable {
	
	private final String type;
	private final String message;
	//stack

	@Override
	public String toString() {
		return "{" + type + ": " + message + "}";
	}
	
	public static ExceptionInfo mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return new ExceptionInfo(t.getClass().getName(), t.getMessage());
		}
		return null;
	}
}
