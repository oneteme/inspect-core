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
	public boolean onCapacityExceeded(boolean complete, QueueResolver resolver) {
		resolver.dequeue(q-> deleteTraces(q, resolver.getQueueCapacity()));
		return true;
	}

	Collection<EventTrace> deleteTraces(Collection<EventTrace> q, int maxCapacity) {
		var size = q.size(); // 1- remove all non complete traces
		if(size > maxCapacity) {
			for(var it=q.iterator(); it.hasNext();) { 
				var t = it.next();
				if(t instanceof CompletableMetric cm) {
					cm.runSynchronizedIfNotComplete(it::remove);  
				}
			} 
			log.debug("{} non-complete traces were deleted", size-q.size());
			size = q.size(); // 2- remove resource usage 
			if(size > maxCapacity) {
				q.removeIf(t-> t instanceof MachineResourceUsage);
				log.debug("{} resource usage traces were deleted", size-q.size());
				size = q.size(); // 3- remove all stages
				if(size > maxCapacity) {
					q.removeIf(t-> t instanceof AbstractStage);
					log.debug("{} stage traces were deleted", size-q.size());
					size = q.size(); // 4- remove all logs
					if(size > maxCapacity) {
						q.removeIf(t-> t instanceof LogEntry);
						log.debug("{} log traces were deleted", size-q.size());
					}
				}
			}
		}
		return q;
	}
}
