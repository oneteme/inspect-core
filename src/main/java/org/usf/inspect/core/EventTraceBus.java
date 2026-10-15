package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static org.usf.inspect.core.Constants.REPORTER;

import java.util.ArrayList;
import java.util.List;

import org.usf.inspect.InstanceEnvironment;

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
		for(var h : dispatchHooks) {
			try {
				h.onInstanceEmit(env);
			}
			catch (Exception e) { //catch exception => next hook
				REPORTER.warn("{}.onInstanceEmit: failed to execute hook", h.getClass().getSimpleName(), e);
			}
		}
	}
	
	public void triggerSchedule(){
		for(var h : dispatchHooks) {
			try {
				h.onSchedule();
			}
			catch (Exception e) { //catch exception => next hook
				REPORTER.warn("{}.onSchedule: failed to execute hook", h.getClass().getSimpleName(), e);
			}
		}
	}
}
