package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface EventListener<T> {

	void onEvent(T state, boolean complete) throws Exception;
}