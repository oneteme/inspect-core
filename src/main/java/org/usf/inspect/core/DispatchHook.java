package org.usf.inspect.core;

import java.util.Collection;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchHook {
	
	default void onInstanceEmit(InstanceEnvironment env) {}

	default void onTraceEmit(EventTrace trace) {}
	
	default void onTracesEmit(Collection<EventTrace> traces) {}
	
	default void preDispatch() {}
	
	default void postDispatch(boolean complete, EventTraceQueueManager resolver) {}
}