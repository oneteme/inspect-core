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
	
	void tryDispatchQueue(int delay, BiFunction<List<EventTrace>, Integer, List<EventTrace>> cons);
	
	void dispatchNow(File file, Callback<Void> cons);

	void dispatchNow(EventTrace[] traces, Callback<Void> cons); //server usage
	
	DispatchState2 getState();
	
	public interface DispatchHook {
		
		default void preRegister(Dispatcher dispatcher, InstanceEnvironment env) {}

		default void postRegister(Dispatcher dispatcher, InstanceEnvironment env) {}
		
		default void onTracesEmit(EventTrace... trace) {}
		
		default void preDispatch(Dispatcher dispatcher) {}
	
		default void postDispatch(Dispatcher dispatcher) {}
	}
}
