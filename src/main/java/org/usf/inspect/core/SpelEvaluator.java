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
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpelEvaluator {

	private static final ExpressionParser PARSER = new SpelExpressionParser();
	private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

	private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, Expression> TEMPLATE_CACHE = new ConcurrentHashMap<>();

	/**
	 * Evaluates a template expression, e.g. {@code "import-#{#file.name}"}.
	 * A value without any {@code #{...}} placeholder is a plain literal and is returned as is (no evaluation).
	 * Used for {@link TraceableStage#name()}.
	 */
	public static String evalMethodTemplate(String template, Object targetObject, Method method, Object[] args) {
		return nonNull(template) && !template.contains("#{") 
				? template //no placeholder
				: eval(template, TEMPLATE_CACHE, ParserContext.TEMPLATE_EXPRESSION, targetObject, method, args);
	}

	/**
	 * Evaluates a pure SpEL expression, e.g. {@code "#id"} or {@code "#user.name"}.
	 * Used for {@link org.springframework.cache.annotation.Cacheable#key()}.
	 */
	public static String evalMethodExpression(String exprValue, Object targetObject, Method method, Object[] args) {
		return eval(exprValue, EXPRESSION_CACHE, null, targetObject, method, args);
	}

	private static String eval(String exprValue, Map<String, Expression> cache, ParserContext parserCtx, Object targetObject, Method method, Object[] args) {
		if(isNull(exprValue) || exprValue.isEmpty()) {
			return method.getName();
		}
		try {
			var exp = cache.computeIfAbsent(exprValue, v-> PARSER.parseExpression(v, parserCtx));
			if(exp instanceof LiteralExpression) { //no placeholder
				return exp.getExpressionString();
			}
			var ctx = new MethodBasedEvaluationContext(targetObject, method, args, PARAM_DISCOVERER);
			ctx.setVariable(method.getDeclaringClass().getSimpleName(), method.getDeclaringClass());
			var val = exp.getValue(ctx);
			if(nonNull(val)) {
				return val.toString();
			}
		} catch (Exception e) {
			log.warn("Failed to evaluate SpEL expression '{}' on {}.{}: {}", exprValue, 
					method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage());
		}
		return method.getName(); //fallback
	}
}
