package org.usf.inspect.core;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface Context {

	InspectCollectorConfiguration getConfiguration();
	
	default boolean dispatch(InstanceEnvironment instance) {return false;}

	default boolean emitTask(DispatchTask task) {return false;}

	default boolean emitTrace(EventTrace trace) {return false;}

	default boolean emitTraces(List<EventTrace> traces) {return false;}

	default void reportError(boolean stack, String action, Throwable thwr) {}

	default void reportMessage(boolean stack, String action, String msg) {}
	
	default boolean isCompleted() {return true;}
}