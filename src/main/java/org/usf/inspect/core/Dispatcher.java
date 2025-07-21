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
	
	void emit(EventTrace trace);
	
	void emitAll(EventTrace[] traces);
	
	boolean dispatchNow(File file);

	boolean dispatchNow(EventTrace[] traces);
	
	void tryDispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons);
	
	DispatchState2 getState();
	
	public interface DispatchHook {
		
		default void preRegister(Dispatcher dispatcher, InstanceEnvironment env) {}

		default void postRegister(Dispatcher dispatcher, InstanceEnvironment env) {}
		
		default void onTraceEmit(EventTrace trace) {}

		default void onTracesEmit(EventTrace[] trace) {}
		
		default void preDispatch(Dispatcher dispatcher) {}
	
		default void postDispatch(Dispatcher dispatcher) {}
	}
}
