package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a single frame in a captured stack trace, recording the class, method, and line number.
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
	
	/**
	 * Returns the Java source file name derived from the class name.
	 *
	 * @return the source file name (e.g. {@code "MyClass.java"})
	 */
	public String getFileName() {
		var bg = className.lastIndexOf('.') + 1;
		var to = className.indexOf('$'); //internal class or anonymous classes
		return className.substring(bg, to > -1 ? to : className.length()) + ".java";
	}
	
	@Override
	public String toString() {
		return format("%s.%s(%s:%d)", className, methodName, getFileName(), lineNumber);
	}

	/**
	 * Builds the stack trace row entries from the given throwable up to the specified maximum number of rows.
	 *
	 * @param thrw the throwable whose stack trace should be captured
	 * @param maxRows the maximum number of frames to capture; negative means all frames
	 * @return the captured stack trace rows, or {@code null} when none are needed
	 */
	public static StackTraceRow[] exceptionStackTraceRows(Throwable thrw, int maxRows) {
		StackTraceRow[] rows = null;
		if(maxRows != 0 && nonNull(thrw)) {
			var stack = thrw.getStackTrace(); 
			rows = new StackTraceRow[maxRows < 0 ? stack.length : min(maxRows, stack.length)];
			for(var i=0; i<rows.length; i++) {
				rows[i] = new StackTraceRow(
						stack[i].getClassName(),
						stack[i].getMethodName(), 
						stack[i].getLineNumber());
			}
		}
		return rows;
	}
	
	/**
	 * Appends formatted stack trace rows to the given string builder.
	 *
	 * @param sb the builder to append to
	 * @param stackRows the rows to format and append
	 */
	public static void appendStackTrace(StringBuilder sb, StackTraceRow[] stackRows) {
		if(nonNull(stackRows)) {
			for(var row : stackRows) {
				sb.append("\n  at ").append(row);
			}
		}
	}
}