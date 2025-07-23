package org.usf.inspect.core;

import org.usf.inspect.core.Dispatcher.DispatchHook;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class EventTracePurger implements DispatchHook {
	
	private final int queueCapacity;
	
	@Override
	public void postDispatch(Dispatcher dispatcher) {
    	dispatcher.tryDispatchQueue(0, (trc, pnd)->{ //will be sent later
    		trc.removeIf(t-> t instanceof CompletableMetric cm && !cm.wasCompleted());
        	if(trc.size() > queueCapacity) {
        		trc.removeIf(t-> t instanceof LogEntry);
            	if(trc.size() > queueCapacity) { //lost details but keep main info
            		trc.removeIf(t-> t instanceof AbstractStage);
            	}// delete StackTraceRows !?
        	}
        	return trc;
    	});
	}
}
