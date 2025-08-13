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
	public boolean onCapacityExceeded(boolean complete, EventTraceQueueManager resolver) {
		resolver.dequeue(q-> deleteTraces(q, resolver.getQueueCapacity()));
		return true;
	}

	Collection<EventTrace> deleteTraces(Collection<EventTrace> traces, int maxCapacity) {
		var size = traces.size(); // 1- remove all non complete traces
		if(size > maxCapacity) {
			for(var it=traces.iterator(); it.hasNext();) { 
				var t = it.next();
				if(t instanceof CompletableMetric cm) {
					cm.runSynchronizedIfNotComplete(it::remove);  
				}
			} 
			log.warn("{} non-complete traces were deleted", size-traces.size());
			size = traces.size(); // 2- remove resource usage 
			if(size > maxCapacity) {
				traces.removeIf(t-> t instanceof MachineResourceUsage);
				log.warn("{} resource usage traces were deleted", size-traces.size());
				size = traces.size(); // 3- remove all stages
				if(size > maxCapacity) {
					traces.removeIf(t-> t instanceof AbstractStage);
					log.warn("{} stage traces were deleted", size-traces.size());
					size = traces.size(); // 4- remove all logs
					if(size > maxCapacity) {
						traces.removeIf(t-> t instanceof LogEntry);
						log.warn("{} log traces were deleted", size-traces.size());
					}
				}
			}
		}
		return traces;
	}
}
