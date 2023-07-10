package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.DefaultUserProvider.isDefaultProvider;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.defaultUserProvider;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.newInstance;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.LaunchMode.BATCH;
import static org.usf.traceapi.core.MainRequest.synchronizedMainRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TraceableAspect {
	
	private final TraceSender sender;
	
    @Around("@annotation(TraceableBatch)")
    public Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
    	if(nonNull(localTrace.get())) { //sub trace
    		return joinPoint.proceed();
    	}
    	Object proceed;
    	var main = synchronizedMainRequest(idProvider.get());
    	localTrace.set(main);
    	var beg = currentTimeMillis();
    	try {
    		proceed = joinPoint.proceed();
    	}
    	catch (Exception e) {
    		main.setException(fromException(e));
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
    			localTrace.remove();
    			main.setLaunchMode(BATCH);
	    		main.setStart(ofEpochMilli(beg));
	    		main.setEnd(ofEpochMilli(fin));
    			main.setName(batchName(joinPoint));
	    		main.setUser(batchUser(joinPoint));
    			main.setThreadName(threadName());
    			main.setApplication(applicationInfo());
	        	sender.send(main);
    		}
    		catch(Exception e) {
				//do not catch exception
				log.warn("error while tracing : {}", joinPoint.getTarget(), e);
    		}
    	}
    	return proceed;
    }
    
    private static String batchName(ProceedingJoinPoint joinPoint) {
    	MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    	var ant = signature.getMethod().getAnnotation(TraceableBatch.class);
    	return ant.value().isBlank() ? signature.getMethod().getName() : ant.value();
    }
    
    private static String batchUser(ProceedingJoinPoint joinPoint) {
    	MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    	var ant = signature.getMethod().getAnnotation(TraceableBatch.class);
    	return isDefaultProvider(ant.userProvider())
    			? defaultUserProvider().getUser()
    			: newInstance(ant.userProvider())
    			.map(BatchUserProvider::getUser)
    			.orElse(null);
    }

}
