package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.MainSession.synchronizedMainSession;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.core.SessionPublisher.emit;

import java.time.Instant;
import java.util.stream.Stream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.ControllerAdvice;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Aspect //TD fork ControllerAdvice & TraceableStage
@RequiredArgsConstructor
public class TraceableAspect {
	
    @ConditionalOnBean(ControllerAdvice.class)
    @Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var session = (RestSession) localTrace.get();
		if(isNull(session)) {
			var sign = joinPoint.getSignature();
			warnNoActiveSession(sign.getName() + "::" + sign.getDeclaringTypeName()); //TD check this
		}
		else if(nonNull(joinPoint.getArgs())) {
			Stream.of(joinPoint.getArgs())
					.filter(Throwable.class::isInstance)
					.findFirst() //trying to find the exception argument
					.map(Throwable.class::cast)
					.map(ExceptionInfo::mainCauseException)
					.ifPresent(session::setException);
		}
		return joinPoint.proceed();
    }
	
    @Around("@annotation(TraceableStage)")
    Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
		var ses = localTrace.get();
    	if(nonNull(ses)) { //sub trace
    		return call(joinPoint::proceed, (s,e,o,t)-> {
    	    	var ss = new RunnableStage();
    			fill(ss, s, e, joinPoint, t);
    			ses.append(ss);
    		});
    	} //TD merge 2 block
    	var ms = synchronizedMainSession();
    	localTrace.set(ms);
    	try {
        	return call(joinPoint::proceed, (s,e,o,t)-> {
    			ms.setType(((MethodSignature)joinPoint.getSignature()).getMethod().getAnnotation(TraceableStage.class).type().toString());
    			fill(ms, s, e, joinPoint, t);
    			emit(ms);
        	});
    	}
    	finally {
			localTrace.remove();
    	}
    }
    
    static void fill(RunnableStage stg, Instant start, Instant end, ProceedingJoinPoint joinPoint, Throwable e) {
    	var ant = ((MethodSignature)joinPoint.getSignature()).getMethod().getAnnotation(TraceableStage.class);
		stg.setStart(start);
		stg.setEnd(end);
		stg.setName(ant.value().isBlank() ? joinPoint.getSignature().getName() : ant.value());
		stg.setLocation(joinPoint.getSignature().getDeclaringTypeName());
		stg.setThreadName(threadName());
		stg.setUser(null); // default user supplier
		stg.setException(mainCauseException(e));
    	if(ant.sessionUpdater() != StageUpdater.class) { //specific.
    		newInstance(ant.sessionUpdater())
    		.ifPresent(u-> u.update(stg, joinPoint));
    	}
    }
}
