package org.usf.inspect.rest;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.usf.inspect.core.RestSession;

/**
 * 
 * @author u$f
 *
 */
@Aspect 
public class ControllerAdviceTracker {

	/**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 */
    @Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var ses = requireCurrentSession(RestSession.class);
		if(nonNull(ses) && nonNull(joinPoint.getArgs())) {
			for(var arg : joinPoint.getArgs()) {
				if(arg instanceof Throwable t) {
					ses.setException(mainCauseException(t));
					break;
				}
			}
		}
		return joinPoint.proceed();
    }
}
