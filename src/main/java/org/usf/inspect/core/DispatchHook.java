package org.usf.inspect.core;

import org.usf.inspect.InstanceEnvironment;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchHook {
	
	default void onInstanceEmit(InstanceEnvironment env) {}

	default void onSchedule() {}
}