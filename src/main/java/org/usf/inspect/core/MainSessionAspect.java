package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.SessionManager.asynclocalRequestListener;
import static org.usf.inspect.core.SessionManager.currentSession;
import static org.usf.inspect.core.SessionManager.endSession;
import static org.usf.inspect.core.SessionManager.startBatchSession;
import static org.usf.inspect.core.SessionPublisher.emit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
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
	
	private final AspectUserProvider userProvider;
	 
    @Around("@annotation(TraceableStage)")
    Object aroundTraceable(ProceedingJoinPoint point) throws Throwable {
		var session = currentSession();
    	return isNull(session) 
    			? aroundBatch(point) 
    			: aroundStage(point);
    }
    
    Object aroundBatch(ProceedingJoinPoint point) throws Throwable {
    	var main = startBatchSession();
    	try {
    		main.setStart(now());
        	main.setThreadName(threadName());
        	main.setUser(userProvider.getUser(point, main.getName()));         
        	var sgn = (MethodSignature)point.getSignature();
    		main.setName(getTraceableName(sgn));
    		main.setLocation(sgn.getDeclaringTypeName());   
    	}
    	finally {
			emit(main);
		}
    	return call(point::proceed, (s,e,o,t)-> {
    		main.submit(ses-> {
    			if(nonNull(t)) {
    				main.appendException(mainCauseException(t));
    			}
    			main.setEnd(e);
    		});
			endSession();
    	});
	}
    
    Object aroundStage(ProceedingJoinPoint point) throws Throwable {
    	var sgn = (MethodSignature)point.getSignature();
    	return call(point::proceed, asynclocalRequestListener(EXEC, 
    			sgn::getDeclaringTypeName,
    			()-> getTraceableName(sgn)));
	}
    
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    Object aroundCacheable(ProceedingJoinPoint point) throws Throwable {
    	var sgn = (MethodSignature)point.getSignature();
    	return call(point::proceed, asynclocalRequestListener(CACHE, 
    			sgn::getDeclaringTypeName,
    			()-> getCacheableName(sgn)));
    }
    
	@Override
	public int getOrder() { //before @Transactional
		return HIGHEST_PRECEDENCE;
	}
	
	static String getTraceableName(MethodSignature sgn) {
    	var ant = sgn.getMethod().getAnnotation(TraceableStage.class);
		return ant.value().isEmpty() ? sgn.getName() : ant.value();
	}

	static String getCacheableName(MethodSignature sgn) {
    	var ant = sgn.getMethod().getAnnotation(Cacheable.class);
		return ant.key().isEmpty() ? sgn.getName() : ant.key();
	}
}
