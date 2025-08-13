package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LocalRequestType.CACHE;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.SessionManager.asynclocalRequestListener;
import static org.usf.inspect.core.SessionManager.createBatchSession;
import static org.usf.inspect.core.SessionManager.currentSession;
import static org.usf.inspect.core.SessionManager.emitSessionEnd;
import static org.usf.inspect.core.SessionManager.emitSessionStart;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

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
public class MethodExecutionMonitor implements Ordered {
	
	private final AspectUserProvider userProvider;
	 
    @Around("@annotation(TraceableStage)")
    Object aroundTraceable(ProceedingJoinPoint point) throws Throwable {
    	return isNull(currentSession()) 
    			? aroundBatch(point) 
    			: aroundStage(point);
    }
    
    Object aroundBatch(ProceedingJoinPoint point) throws Throwable {
    	var ses = createBatchSession();
    	try {
    		ses.setType(BATCH.name());
    		ses.setStart(now());
        	ses.setThreadName(threadName());        
        	var sgn = (MethodSignature)point.getSignature();
    		ses.setName(getTraceableName(sgn));
    		ses.setLocation(sgn.getDeclaringTypeName());   
        	ses.setUser(userProvider.getUser(point, ses.getName()));
		} catch (Exception t) {
			context().reportEventHandleError(ses.getId(), t);
		}
		emitSessionStart(ses);
    	return call(point::proceed, (s,e,o,t)-> {
    		ses.runSynchronized(()-> {
				if(nonNull(t)) {
					ses.setException(fromException(t));
				}
				ses.setEnd(e);
    		});
    		emitSessionEnd(ses);
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
    
    /**
	 * Filter → Interceptor.preHandle → Controller → (ControllerAdvice if exception) → Interceptor.postHandle → View → Interceptor.afterCompletion → Filter (end).
	 */
    @Deprecated(forRemoval = true) //@see HandlerExceptionResolverMonitor
    //@Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var ses = requireCurrentSession(RestSession.class);
		if(nonNull(ses) && nonNull(joinPoint.getArgs())) {
			for(var arg : joinPoint.getArgs()) {
				if(arg instanceof Throwable t) {
					ses.runSynchronized(()-> ses.setException(fromException(t)));
					break;
				}
			}
		}
		return joinPoint.proceed();
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
