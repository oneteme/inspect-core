package org.usf.inspect.core;

import java.util.Collection;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchHook {
	
	default void onInstanceEmit(InstanceEnvironment env) {}

	default void onSchedule(Context ctx) {}
	
	default void onDispatch(Context ctx, Collection<EventTrace> traces) {}
}