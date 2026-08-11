package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 * Dispatches trace lifecycle events to registered hooks.
 */
@Slf4j
public final class EventTraceBus {

	private final List<DispatchHook> dispatchHooks = synchronizedList(new ArrayList<>());
	
	/**
	 * Registers a dispatch hook.
	 *
	 * @param hook the hook to register
	 */
	public void registerHook(DispatchHook hook) {
		dispatchHooks.add(hook);
	}
	
	/**
	 * Removes a dispatch hook.
	 *
	 * @param hook the hook to remove
	 */
	public void removeHook(DispatchHook hook) {
		dispatchHooks.remove(hook);
	}
	/**
	 * Notifies hooks that instance environment information was emitted.
	 *
	 * @param env the emitted instance environment
	 */
	public void triggerInstanceEmit(InstanceEnvironment env){
		triggerHooks(h-> h.onInstanceEmit(env));
	}
	
	/**
	 * Notifies hooks that scheduled dispatching is about to run.
	 *
	 * @param ctx the trace hub context
	 */
	public void triggerSchedule(TraceHub ctx){
		triggerHooks(h-> h.onSchedule(ctx));
	}
	
	/**
	 * Notifies hooks that traces are being dispatched.
	 *
	 * @param ctx the trace hub context
	 * @param traces the traces being dispatched
	 */
	public void triggerTraceDispatch(TraceHub ctx, List<EventTrace> traces){
		triggerHooks(h-> h.onDispatch(ctx, traces));
	}
	
	void triggerHooks(Consumer<? super DispatchHook> post){
		dispatchHooks.forEach(h -> {
			try {
				post.accept(h);
			}
			catch (Exception e) { //catch exception => next hook
				log.warn("failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		});
	}
}
