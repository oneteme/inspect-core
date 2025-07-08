package org.usf.inspect.core;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 
 * @author u$f
 *
 */
public interface AspectUserProvider {
	
	default String getUser(ProceedingJoinPoint point, String stageName) {
		return null;
	}
}
