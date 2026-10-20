package org.usf.inspect;

import static java.lang.String.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class StackTraceRow {
		
	private final String className;
	private final String methodName;
	private final int lineNumber;
	
	public String resolveFileName() {
		var bg = className.lastIndexOf('.') + 1;
		var to = className.indexOf('$'); //internal class or anonymous classes
		return className.substring(bg, to > -1 ? to : className.length()) + ".java";
	}
	
	@Override
	public String toString() {
		return format("%s.%s(%s:%d)", className, methodName, resolveFileName(), lineNumber);
	}
}