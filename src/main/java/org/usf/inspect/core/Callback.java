package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface Callback<T> {

	void accept(T obj, Throwable t);
}
