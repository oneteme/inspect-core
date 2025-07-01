package org.usf.inspect.core;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface AspectUserProvider {
	
	String getUser(ProceedingJoinPoint point, String stageName);

    static String getAspectUser(ProceedingJoinPoint point, String stageName){
    	return null;
    }
}
