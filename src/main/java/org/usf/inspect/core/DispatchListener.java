package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface DispatchListener {

	void onDispatchEvent(DispatchState state, boolean complete) throws Exception;
}