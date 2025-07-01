package org.usf.inspect.rest;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import java.util.stream.Stream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestSession;

/**
 * 
 * @author u$f
 *
 */
@Aspect 
public class ControllerAdviceTracker {

    @Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var ses = requireCurrentSession(RestSession.class);
		if(nonNull(ses) && nonNull(joinPoint.getArgs())) {
			Stream.of(joinPoint.getArgs())
					.filter(Throwable.class::isInstance)
					.findFirst() //trying to find the exception argument
					.map(Throwable.class::cast)
					.map(ExceptionInfo::mainCauseException)
					.ifPresent(ses::appendException);
		}
		return joinPoint.proceed();
    }
}
