package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.Helper.newInstance;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.currentSession;
import static org.usf.inspect.core.SessionManager.endSession;
import static org.usf.inspect.core.SessionManager.localRequestCreator;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.SessionManager.startBatchSession;
import static org.usf.inspect.core.SessionPublisher.emit;
import static org.usf.inspect.core.StageTracker.call;

import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Aspect
@RequiredArgsConstructor
public class MainSessionAspect implements Ordered {
	
    @Around("@annotation(TraceableStage)")
    Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
		var ses = currentSession();
    	if(isNull(ses)) { //STARTUP session
        	var ms = startBatchSession();
        	try {
            	return call(joinPoint::proceed, (s,e,o,t)-> {
        			fill(ms, s, e, joinPoint, t);
        			emit(ms);
            	});
        	}
        	finally {
    			endSession();
        	}
    	}
    	return call(joinPoint::proceed, (s,e,o,t)-> {
	    	var req = new LocalRequest();
			fill(req, s, e, joinPoint, t);
			return req;
		}, requestAppender());
    }
    
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
    	var sign = joinPoint.getSignature();
    	return call(joinPoint::proceed, 
    			localRequestCreator(sign.getName(), formatLocation(sign.getDeclaringTypeName(), sign.getName()), null), 
    			requestAppender());
    }
    
    static void fill(LocalRequest stg, Instant start, Instant end, ProceedingJoinPoint joinPoint, Throwable e) {
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

	@Override
	public int getOrder() { //before @Transactional
		return HIGHEST_PRECEDENCE;
	}
}
