package org.usf.inspect.core;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 
 * @author u$f
 *
 */
public interface Dispatcher {
	
	boolean emit(EventTrace trace);
	
	boolean emitAll(EventTrace[] traces);
	
	void tryPropagateQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons);
	
	void dispatchNow(File file, int attempts, Callback<Void> cons);
	
	DispatchState2 getState();
	
	public interface DispatchHook {
		
		default void onInstanceEmit(InstanceEnvironment env) {}

		default void onTraceEmit(EventTrace trace) {}
		
		default void onTracesEmit(EventTrace[] trace) {}
		
		default void preDispatch(Dispatcher dispatcher) {}
	
		default void postDispatch(Dispatcher dispatcher) {}
	}
}
