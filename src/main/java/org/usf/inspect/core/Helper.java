package org.usf.inspect.core;

import static java.lang.Thread.currentThread;

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

	public static String formatLocation(String className, String methodName) {
		return className + '.' + methodName + "()";
	}
}