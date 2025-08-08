package org.usf.inspect.core;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchHook {
	
	default void onInstanceEmit(InstanceEnvironment env) {}

	default void onTraceEmit(EventTrace trace) {}
	
	default void onTracesEmit(EventTrace[] traces) {}
	
	default void onDispatch(boolean complete, List<EventTrace> traces) {}
	
	default boolean onCapacityExceeded(EventTrace[] traces) { return false;}
}