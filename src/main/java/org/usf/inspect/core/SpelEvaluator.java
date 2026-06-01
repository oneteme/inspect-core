package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpelEvaluator {

	private static final ExpressionParser PARSER = new SpelExpressionParser();
	private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

	private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

	public static String evalMethodExpression(String exprValue, Object targetObject, Method method, Object[] args) {
		if (isNull(exprValue)|| exprValue.isEmpty()) {
			return method.getName();
		}
		try {
			var ext = EXPRESSION_CACHE.computeIfAbsent(exprValue, PARSER::parseExpression);
			var ctx = new MethodBasedEvaluationContext(targetObject, method, args, PARAM_DISCOVERER);
			ctx.setVariable(method.getDeclaringClass().getSimpleName(), method.getDeclaringClass());
			var val = ext.getValue(ctx);
			if(nonNull(val)) {
				return val.toString();
			}
		} catch (Exception e) {
			log.warn("Failed to evaluate SpEL expression '{}': {}", exprValue, e.getMessage());
		}
		return exprValue;
	}
}
