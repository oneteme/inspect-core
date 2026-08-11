package org.usf.inspect.core;

import java.util.List;

/**
 * Coordinates trace collection, dispatching, and reporting operations.
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface TraceHub {

	/**
	 * Returns the collector configuration used by this hub.
	 * 
	 * @return the active collector configuration
	 */
	InspectCollectorConfiguration getConfiguration();
	
	/**
	 * Dispatches instance metadata through this hub.
	 * 
	 * @param instance the instance environment to dispatch
	 * @return {@code true} if the instance was handled
	 */
	default boolean dispatch(InstanceEnvironment instance) {return false;}

	/**
	 * Emits a dispatch task through this hub.
	 * 
	 * @param task the dispatch task to emit
	 * @return {@code true} if the task was handled
	 */
	default boolean emitTask(DispatchTask task) {return false;}

	/**
	 * Emits a single trace through this hub.
	 * 
	 * @param trace the trace to emit
	 * @return {@code true} if the trace was handled
	 */
	default boolean emitTrace(EventTrace trace) {return false;}

	/**
	 * Emits a list of traces through this hub.
	 * 
	 * @param traces the traces to emit
	 * @return {@code true} if the traces were handled
	 */
	default boolean emitTraces(List<EventTrace> traces) {return false;}

	/**
	 * Reports an error that occurred while processing an action.
	 * 
	 * @param stack whether stack details should be included
	 * @param action the action being performed when the error occurred
	 * @param thwr the error to report
	 */
	default void reportError(boolean stack, String action, Throwable thwr) {}

	/**
	 * Reports a message related to a processing action.
	 * 
	 * @param stack whether stack details should be included
	 * @param action the related action
	 * @param msg the message to report
	 */
	default void reportMessage(boolean stack, String action, String msg) {}
	
	/**
	 * Indicates whether this hub has completed its work.
	 * 
	 * @return {@code true} if processing is complete
	 */
	default boolean isCompleted() {return true;}
}