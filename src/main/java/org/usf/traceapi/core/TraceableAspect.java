package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.Helper.hostAddress;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.operatingSystem;
import static org.usf.traceapi.core.Helper.runtimeEnviroment;
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
    			main.setThread(threadName());
	        	main.setOs(operatingSystem());
	        	main.setRe(runtimeEnviroment());
	        	main.setLocation(hostAddress()); //IP address
	        	main.setFailed(failed);
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

}
