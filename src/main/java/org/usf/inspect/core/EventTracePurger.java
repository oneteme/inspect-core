package org.usf.inspect.core;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class EventTracePurger implements DispatchHook {
	
	@Override
	public void postDispatch(boolean complete, EventTraceQueueManager manager) {
		if(manager.isQueueCapacityExceeded()) {
			manager.dequeue(q-> deleteTraces(q, manager.getQueueCapacity()));
		}
	}

	Collection<EventTrace> deleteTraces(Collection<EventTrace> traces, int maxCapacity) {
		log.debug("queue.size = {} > queue.capacity = {}", traces.size(), maxCapacity);
		for(var it=traces.iterator(); it.hasNext();) { 
			if(it.next() instanceof CompletableMetric cm) {
				cm.runSynchronizedIfNotComplete(it::remove);  
			}
		}
		deletedTracesByType(traces, maxCapacity, MachineResourceUsage.class, AbstractStage.class, LogEntry.class);
		return traces;
	}
	
	void deletedTracesByType(Collection<EventTrace> traces, int maxCapacity, Class<?>... types) {
		for(var t : types) {
			var pre = traces.size();
			if(pre > maxCapacity) {
				traces.removeIf(t::isInstance);
				if(pre > traces.size()) {
					log.warn("{} {} traces were deleted", pre - traces.size(), t.getSimpleName());
				}
			}
			else {
				break;
			}
		}
	}
}
