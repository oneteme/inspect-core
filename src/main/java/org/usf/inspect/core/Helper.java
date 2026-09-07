package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.Objects.nonNull;

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
	
	public static String threadName() {
		var t = currentThread(); //java17 ?
		return t.isVirtual() ?  ""+t.threadId() : t.getName();
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

	public static String formatLocation(String className, String methodName) {
		return className + '.' + methodName + "()";
	}
}