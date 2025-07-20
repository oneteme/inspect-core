package org.usf.inspect.core;

import org.usf.inspect.core.Dispatcher.DispatchHook;

/**
 * 
 * @author u$f
 *
 */
public final class EventTracePurger implements DispatchHook {
	
	@Override
	public void postDispatch(Dispatcher dispatcher) {
    	dispatcher.tryReduceQueue(0, (trc, max)->{ //will be sent later
    		trc.removeIf(t-> t instanceof CompletableMetric c && !c.wasCompleted());
        	if(trc.size() > max) {
        		trc.removeIf(t-> t instanceof LogEntry);
            	if(trc.size() > max) { //lost details but keep main info
            		trc.removeIf(t-> t instanceof AbstractStage);
                	if(trc.size() > max) { 
                		trc = trc.subList(0, max);
                	}
            	}
        	}
        	return trc;
    	});
	}
}
