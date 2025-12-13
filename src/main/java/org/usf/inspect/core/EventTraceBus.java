package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class EventTraceBus {

	private final List<DispatchHook> dispatchHooks = synchronizedList(new ArrayList<>());
	
	public void registerHook(DispatchHook hook) {
		dispatchHooks.add(hook);
	}
	
	public void removeHook(DispatchHook hook) {
		dispatchHooks.remove(hook);
	}
	public void triggerInstanceEmit(InstanceEnvironment env){
		triggerHooks(dispatchHooks, h -> h.onInstanceEmit(env));
	}
	
	public void triggerSchedule(Context ctx){
		triggerHooks(dispatchHooks, h-> h.onSchedule(ctx));
	}
	
	public void triggerTraceDispatch(Context ctx, List<EventTrace> traces){
		triggerHooks(dispatchHooks, h-> h.onTraceDispatch(ctx, traces));
	}
	
	static <T> void triggerHooks(List<T> hooks, Consumer<? super T> post){
		hooks.forEach(h -> {
			try {
				post.accept(h);
			}
			catch (Exception e) { //catch exception => next hook
				log.warn("failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		});
	}
}
