package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.MainSession.synchronizedMainSession;
import static org.usf.traceapi.core.MetricsTracker.supply;
import static org.usf.traceapi.core.Session.nextId;
import static org.usf.traceapi.core.TraceMultiCaster.emit;

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
@Aspect
@RequiredArgsConstructor
public class TraceableAspect {
	
    @ConditionalOnBean(ControllerAdvice.class)
    @Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var session = (ApiSession) localTrace.get();
		if(nonNull(session) && nonNull(joinPoint.getArgs())) {
			Stream.of(joinPoint.getArgs())
					.filter(Throwable.class::isInstance)
					.findFirst()
					.map(Throwable.class::cast)
					.map(ExceptionInfo::mainCauseException)
					.ifPresent(session::setException);
		}
		return joinPoint.proceed();
    }
	
    @Around("@annotation(TraceableStage)")
    Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
		var session = localTrace.get();
    	if(nonNull(localTrace.get())) { //sub trace
    		log.trace("stage : {} <= {}", session.getId(), joinPoint.getSignature());
    		return supply(joinPoint::proceed, (s,e,o,t)-> {
    	    	var rs = new RunnableStage();
    			fill(rs, s, e, joinPoint, t);
    			session.append(rs);
    		});
    	} //TD merge 2 block
    	var ms = synchronizedMainSession(nextId());
    	localTrace.set(ms);
    	log.trace("session : {} <= {}", ms.getId(), joinPoint.getSignature());
    	try {
        	return supply(joinPoint::proceed, (s,e,o,t)-> {
    			ms.setType(((MethodSignature)joinPoint.getSignature()).getMethod().getAnnotation(TraceableStage.class).type().toString());
    			ms.setApplication(applicationInfo());
    			fill(ms, s, e, joinPoint, t);
    			emit(ms);
        	});
    	}
    	finally {
			localTrace.remove();
    	}
    }

    static void fill(RunnableStage sg, Instant beg, Instant fin, ProceedingJoinPoint joinPoint, Throwable e) {
    	var ant = ((MethodSignature)joinPoint.getSignature()).getMethod().getAnnotation(TraceableStage.class);
		sg.setStart(beg);
		sg.setEnd(fin);
		sg.setName(ant.value().isBlank() ? joinPoint.getSignature().getName() : ant.value());
		sg.setLocation(joinPoint.getSignature().getDeclaringTypeName());
		sg.setThreadName(threadName());
		sg.setUser(null); // default user supplier
		sg.setException(mainCauseException(e));
    	if(ant.sessionUpdater() != StageUpdater.class) { //specific.
    		newInstance(ant.sessionUpdater())
    		.ifPresent(u-> u.update(sg, joinPoint));
    	}
    }
}
