package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
//@FunctionalInterface
public interface TraceHandler<T> {

	void handle(T obj);
	
	default void complete() throws Exception {}
}