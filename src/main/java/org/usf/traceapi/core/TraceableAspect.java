package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.LaunchMode.BATCH;
import static org.usf.traceapi.core.MainSession.synchronizedMainSession;
import static org.usf.traceapi.core.Session.nextId;
import static org.usf.traceapi.core.TraceMultiCaster.emit;

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
    		return aroundStage(joinPoint, session);
    	}
    	var ms = synchronizedMainSession(nextId());
    	localTrace.set(ms);
    	log.debug("session : {} <= {}", ms.getId(), joinPoint.getSignature());
    	Throwable ex = null;
    	var beg = currentTimeMillis();
    	try {
    		return joinPoint.proceed();
    	}
    	catch (Throwable e) {
    		ex =  e;
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
    			ms.setLaunchMode(BATCH);
    			ms.setApplication(applicationInfo());
    			fill(ms, beg, fin, joinPoint, ex);
    			emit(ms);
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
    	    	var rs = new RunnableStage();
    			fill(rs, beg, fin, joinPoint, ex);
				session.append(rs);
    		}
    		catch(Exception e) {
				log.warn("error while tracing : " + joinPoint.getSignature(), e);
				//do not throw exception
    		}
			session.unlock();
    	}
    }
    
    static void fill(RunnableStage sg, long beg, long fin, ProceedingJoinPoint joinPoint, Throwable e) {
    	MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    	var ant = signature.getMethod().getAnnotation(TraceableStage.class);
		sg.setStart(ofEpochMilli(beg));
		sg.setEnd(ofEpochMilli(fin));
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
