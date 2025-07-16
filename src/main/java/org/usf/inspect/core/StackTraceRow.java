package org.usf.inspect.core;

import static java.lang.Math.min;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class StackTraceRow {
		
	private final String className;
	private final String methodName;
	private final int lineNumber;
	
	@Override
	public String toString() {
		var from = className.lastIndexOf('.') + 1;
		var to = className.indexOf('$');
		var fn = to > -1 ? className.substring(from, to) : className.substring(from);
		return className + '.' + methodName + '(' + fn + ".java:" + lineNumber + ')';
	}
	
	static StackTraceRow[] fromStackTrace(StackTraceElement[] stack, int maxStack) {
		var stackRows = new StackTraceRow[min(maxStack, stack.length)];
		for(var i=0; i<stackRows.length; i++) {
			stackRows[i] = new StackTraceRow(
					stack[i].getClassName(),
					stack[i].getMethodName(), 
					stack[i].getLineNumber());
		}
		return stackRows;
	}
}
