package org.usf.inspect.core;

import java.io.File;
import java.util.List;
import java.util.function.BiFunction;

public interface Dispatcher {
	
	void emit(EventTrace trace);
	
	void emitAll(EventTrace[] traces);
	
	boolean dispatch(File file);
	
	void tryReduceQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons);
	
	DispatchState getState();
	
	public interface DispatchHook {
		
		default void onInit(InstanceEnvironment env) {}
		
		default void onTrace(EventTrace trace) {}
		
		default void preDispatch(Dispatcher dispatcher) {}
	
		default void postDispatch(Dispatcher dispatcher) {}
	}
}
