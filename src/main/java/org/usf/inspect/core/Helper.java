package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Helper {
	
	private static final String ROOT_PACKAGE = Helper.class.getPackageName();
	
	public static String threadName() {
		return currentThread().getName();
	}
	
	public static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("\\w+ .+") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
	
	public static Optional<StackTraceElement> outerStackTraceElement() {
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		return i<arr.length ? Optional.of(arr[i]) : empty();
	}
	
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
}