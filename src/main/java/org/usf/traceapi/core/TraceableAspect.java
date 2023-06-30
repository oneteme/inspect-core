package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.Helper.defaultUserProvider;
import static org.usf.traceapi.core.Helper.hostAddress;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.LaunchMode.BATCH;
import static org.usf.traceapi.core.MainRequest.synchronizedMainRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class TraceableAspect {
	
	private final TraceSender sender;
	
    @Around("@annotation(TraceableBatch)")
    public Object aroundBatch(ProceedingJoinPoint joinPoint) throws Throwable {
    	if(nonNull(localTrace.get())) { //sub trace
    		return joinPoint.proceed();
    	}
    	Object proceed;
    	var failed = true;
    	var main = synchronizedMainRequest(idProvider.get());
    	localTrace.set(main);
    	var beg = currentTimeMillis();
    	try {
    		proceed = joinPoint.proceed();
    		failed = false;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
    			localTrace.remove();
    			main.setName(batchName(joinPoint));
    			main.setLaunchMode(BATCH);
	    		main.setStart(ofEpochMilli(beg));
	    		main.setEnd(ofEpochMilli(fin));
	        	main.setLocation(hostAddress()); //IP address
	        	main.setFailed(failed);
	    		main.setUser(batchUser(joinPoint));
    			main.setThreadName(threadName());
    			main.setApplication(Helper.applicationInfo());
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
    
    private static String batchUser(ProceedingJoinPoint joinPoint) { //simple impl.
    	MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    	var ant = signature.getMethod().getAnnotation(TraceableBatch.class);
    	BatchUserProvider provider;
    	if(ant.userProvider() == DefaultUserProvider.class) { // return null by default
    		provider = defaultUserProvider();
    	}
    	else {
    		try {
    			provider = ant.userProvider().getDeclaredConstructor().newInstance();
    		} catch (Exception e) {
    			log.warn("cannot instantiate class " + ant.userProvider(), e);
    			return null;
    		}
    	}
    	return provider.getUser();
    }

}
