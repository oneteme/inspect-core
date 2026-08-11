package org.usf.inspect.core;

import java.util.Collection;

/**
 * Receives lifecycle callbacks during trace dispatch.
 */
public interface DispatchHook {
	
	/**
	 * Invoked when instance environment information is emitted.
	 *
	 * @param env the emitted instance environment
	 */
	default void onInstanceEmit(InstanceEnvironment env) {}

	/**
	 * Invoked before scheduled trace dispatching begins.
	 *
	 * @param ctx the trace hub context
	 */
	default void onSchedule(TraceHub ctx) {}
	
	/**
	 * Invoked when a batch of traces is about to be dispatched.
	 *
	 * @param ctx the trace hub context
	 * @param traces the traces being dispatched
	 */
	default void onDispatch(TraceHub ctx, Collection<EventTrace> traces) {}
}