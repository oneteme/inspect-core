package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.LaunchMode.BATCH;
import static org.usf.traceapi.core.MainSession.synchronizedMainRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
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
	
	private final TraceSender sender;
	
    @Around("@annotation(TraceableBatch)")
    Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
		var session = localTrace.get();
    	if(nonNull(localTrace.get())) { //sub trace
    		return aroundStage(joinPoint, session);
    	}
    	var ms = synchronizedMainRequest(idProvider.get());
    	localTrace.set(ms);
    	log.debug("session : {} <= {}", ms.getId(), joinPoint.getSignature());
		Exception ex = null;
    	var beg = currentTimeMillis();
    	try {
    		return joinPoint.proceed();
    	}
    	catch (Exception e) {
    		ex =  e;
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
    			ms.setLaunchMode(BATCH);
    			ms.setApplication(applicationInfo());
    			fill(ms, beg, fin, joinPoint, ex);
	        	sender.send(ms);
    		}
    		catch(Exception e) {
				log.warn("error while tracing : " + joinPoint.getSignature(), e);
				//do not throw exception
    		}
			localTrace.remove();
    	}
    }

    static Object aroundStage(ProceedingJoinPoint joinPoint, Session session) throws Throwable {
		session.lock();
		log.debug("stage : {} <= {}", session.getId(), joinPoint.getSignature());
		Exception ex = null;
    	var beg = currentTimeMillis();
    	try {
    		return joinPoint.proceed();
    	}
    	catch (Exception e) {
    		ex =  e;
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
    	    	var sg = new RunnableStage();
    			fill(sg, beg, fin, joinPoint, ex);
				session.append(sg);
    		}
    		catch(Exception e) {
				log.warn("error while tracing : " + joinPoint.getSignature(), e);
				//do not throw exception
    		}
			session.unlock();
    	}
    }
    
    @ConditionalOnBean(ControllerAdvice.class)
    @Around("within(@org.springframework.web.bind.annotation.ControllerAdvice *)")
    Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
		var session = (ApiSession) localTrace.get();
		if(session != null) {
			session.setException(fromException((Throwable)joinPoint.getArgs()[0]));
		}
		return joinPoint.proceed();
    }
    
    static void fill(RunnableStage sg, long beg, long fin, ProceedingJoinPoint joinPoint, Exception e) {
    	MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    	var ant = signature.getMethod().getAnnotation(TraceableBatch.class);
		sg.setStart(ofEpochMilli(beg));
		sg.setEnd(ofEpochMilli(fin));
		sg.setName(ant.value().isBlank() ? joinPoint.getSignature().getName() : ant.value());
		sg.setLocation(ant.value().isBlank() ? joinPoint.getSignature().getDeclaringTypeName() : ant.location());
		sg.setThreadName(threadName());
		sg.setUser(null); // default user supplier
		sg.setException(fromException(e));
    	if(ant.sessionUpdater() != StageUpdater.class) { //specific.
    		newInstance(ant.sessionUpdater()).ifPresent(su-> su.update(sg, joinPoint));
    	}
    }
}
