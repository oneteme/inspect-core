package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SessionHandler<T> {

	void handle(T obj);
	
	default void complete() throws Exception {}
}