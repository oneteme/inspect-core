package org.usf.inspect.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;


/**
 * Shared utility methods for trace collection and formatting.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Helper {
	
	private static final String ROOT_PACKAGE = Helper.class.getPackageName();
	
	/**
	 * Returns the current thread name or virtual thread identifier.
	 *
	 * @return the current thread label
	 */
	public static String threadName() {
		var t = currentThread();
		return t.isVirtual() ?  ""+t.threadId() : t.getName();
	}
	
	/**
	 * Extracts the authentication scheme from an authorization header.
	 *
	 * @param authHeader the authorization header value
	 * @return the extracted authentication scheme, or {@code null} when unavailable
	 */
	public static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("\\w+ .+") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
	
	/**
	 * Returns the first stack trace element outside the inspect package.
	 *
	 * @return the outer stack trace element, if available
	 */
	public static Optional<StackTraceElement> outerStackTraceElement() {
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		return i<arr.length ? Optional.of(arr[i]) : empty();
	}
	
	/**
	 * Counts the number of elements in a collection, map, or array.
	 *
	 * @param o the object to inspect
	 * @return the element count, or {@code -1} when the object type is unsupported
	 */
	public static int count(Object o) {
		if(nonNull(o)) {
			if(o instanceof Collection<?> c) {
				return c.size();
			}
			if(o instanceof Map<?,?> m) {
				return m.size();
			}
			if(o.getClass().isArray()) {
				return getLength(o);
			}
		}
		return -1;
	}

	//e.g. batch name (arg param)
	@Deprecated(since="0.4.0", forRemoval=true) //use SpelEvaluator.evalMethodExpression instead
	public static Object evalExpression(String exp, Object root, Class<?> clazz, String[] params, Object[] args) {
		if(exp.contains("#")) {
			var ctx = new StandardEvaluationContext(root);
			ctx.setVariable(clazz.getSimpleName(), clazz); //static fields/methods
			if(nonNull(params) && nonNull(args)) {
				var n = min(params.length, args.length);
				for(int i=0; i<n; i++) {
					ctx.setVariable(params[i], args[i]);
				}
			}				
	        return new SpelExpressionParser().parseExpression(exp).getValue(ctx);
		}
		return exp;
	}

	/**
	 * Formats a class and method name as a location string.
	 *
	 * @param className the declaring class name
	 * @param methodName the declaring method name
	 * @return the formatted location
	 */
	public static String formatLocation(String className, String methodName) {
		return className + '.' + methodName + "()";
	}
}