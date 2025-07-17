package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

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
	
	public String getFileName() {
		var bg = className.lastIndexOf('.') + 1;
		var to = className.indexOf('$'); //internal class or anonymous classes
		return className.substring(bg, to > -1 ? to : className.length()) + ".java";
	}
	
	@Override
	public String toString() {
		return format("%s.%s(%s:%d)", className, methodName, getFileName(), lineNumber);
	}

	public static StackTraceRow[] threadStackTraceRows(int maxRows) {
		return maxRows > 0 
				? excetionStackTraceRows(new Exception(), maxRows) //see Thread.getStackTrace
				: null;
	}
	
	public static StackTraceRow[] excetionStackTraceRows(Throwable thrw, int maxRows) {
		StackTraceRow[] rows = null;
		if(maxRows > 0) {
			var stack = thrw.getStackTrace(); 
			rows = new StackTraceRow[min(maxRows, stack.length)];
			for(var i=0; i<rows.length; i++) {
				rows[i] = new StackTraceRow(
						stack[i].getClassName(),
						stack[i].getMethodName(), 
						stack[i].getLineNumber());
			}
		}
		return rows;
	}
	
	public static void appendStackTrace(StringBuilder sb, StackTraceRow[] stackRows) {
		if(nonNull(stackRows)) {
			for(var row : stackRows) {
				sb.append("\n  at ").append(row);
			}
		}
	}
}
