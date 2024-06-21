package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SessionHandler<T> {

	void handle(T session);
	
	default void complete() throws Exception {}
}