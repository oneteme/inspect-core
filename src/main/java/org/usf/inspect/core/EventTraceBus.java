package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static org.usf.inspect.core.Constants.REPORTER;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 
 * @author u$f
 *
 */
public final class EventTraceBus {

	private final List<DispatchHook> dispatchHooks = synchronizedList(new ArrayList<>());
	
	public void registerHook(DispatchHook hook) {
		dispatchHooks.add(hook);
	}
	
	public void triggerInstanceEmit(InstanceEnvironment env){
		triggerHooks(h-> h.onInstanceEmit(env));
	}
	
	public void triggerSchedule(){
		triggerHooks(DispatchHook::onSchedule);
	}
	
	void triggerHooks(Consumer<? super DispatchHook> post){
		dispatchHooks.forEach(h -> {
			try {
				post.accept(h);
			}
			catch (Exception e) { //catch exception => next hook
				REPORTER.warn("failed to execute hook '{}'", h.getClass().getSimpleName());
			}
		});
	}
}
