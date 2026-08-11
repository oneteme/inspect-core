package org.usf.inspect.core;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Resolves a user name from an aspect join point.
 */
public interface AspectUserProvider {
	
	/**
	 * Resolves the current user for the intercepted stage.
	 *
	 * @param point the intercepted join point
	 * @param stageName the current stage name
	 * @return the resolved user name, or {@code null} when unavailable
	 */
	default String getUser(ProceedingJoinPoint point, String stageName) {
		return null;
	}
}
