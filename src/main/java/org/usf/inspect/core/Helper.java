package org.usf.inspect.core;

import static java.lang.StackWalker.getInstance;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.lang.Thread.currentThread;

import java.lang.StackWalker.StackFrame;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Helper {
	
	public static String threadName() {
		var t = currentThread(); //java17 ?
		return t.isVirtual() ?  ""+t.threadId() : t.getName();
	}

	public static String formatLocation(String className, String methodName, int lineNumber) {
		return formatLocation(className, methodName) + ':' + lineNumber;
	}

	public static String formatLocation(String className, String methodName) {
		return className + '.' + methodName + "()";
	}
	
	public static StackFrame upperStackFrame(Class<?> typeToSkip) {
		return getInstance(RETAIN_CLASS_REFERENCE).walk(fr-> fr
				.skip(1) //skip Helper.upperStackFrame first
				.dropWhile(f-> f.getDeclaringClass() == typeToSkip) //skip type
				.findFirst().orElse(null));
	}
}