package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.DispatchState.DISPATCH;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class EventTraceDispatcher implements EventListener<DispatchState> {
	
	private final TracingProperties prop;
    private final ConcurrentLinkedSetQueue<EventTrace> queue;
    private final DumpFileDispatcher dumpService; //optional, can be null
    private final DispatcherAgent agent;
    private int attempts;
    
	@Override
	public final void onEvent(DispatchState state, boolean complete) throws Exception {
    	if(state == DISPATCH) {
			dispatch(complete);
		}
    	else {
    		log.warn("cannot emit items as the dispatcher state is {}, current queue size: {}", state, queue.size());
    	}
    	if(prop.getQueueCapacity() > 0 && (state != DISPATCH || attempts > 0)) { // !DISPACH | dispatch=fail
    		dumpTraces(); //dump traces if queue size exceeds capacity
    	}
    }
	
	void dumpTraces() {
		if(queue.size() >= prop.getQueueCapacity()) {
			log.warn("queue size {} exceeds capacity {}, dumping items to file", queue.size(), prop.getQueueCapacity());
			try {
				dumpService.dump(queue.pop());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //dump all items in the queue
    	}
	}
	
   void dispatch(boolean complete) {
    	var cs = queue.pop();
        log.trace("dispatching {} items, current queue size: {}", cs.size(), queue.size());
        try {
        	var modifiable = new ArrayList<>(cs);
        	var pending = extractPendingMetrics(prop.getDelayIfPending(), modifiable);
        	agent.dispatch(complete, ++attempts, pending.size(), modifiable);
    		if(attempts > 1) { //more than one attempt
    			log.info("successfully dispatched {} items after {} attempts", cs.size(), attempts);
    			dispatchDump(); //dispatch dump files
    		}
    		attempts=0;
    		cs = pending; //keep pending traces for next dispatch
    	}
    	catch (Exception e) {// do not throw exception : retry later
    		log.warn("failed to dispatch {} items after {} attempts, cause: [{}] {}", 
    				cs.size(), attempts, e.getClass().getSimpleName(), e.getMessage()); //do not log exception stack trace
    	}
        catch (OutOfMemoryError e) {
			cs = emptyList(); //do not add items back to the queue, may release memory
			attempts = 0;
			log.error("out of memory error while dispatching {} items, those will be aborted", cs.size());
        }
        finally { //TODO dump file after 100 attempts or 90% max buffer size
        	if(!cs.isEmpty()) {
	        	queue.requeueAll(cs); //go back to the queue (preserve order)
        	}
        }
    }
   
   void dispatchDump() {
		dumpService.forEachDump(f->{
			try {
				log.debug("dispatching dump file {}", f.getName());
				agent.dispatch(f); //dispatch dump file
				return true;
			} catch (Exception e) {
				log.warn("cannot dispatch dump file {}, cause: [{}] {}", f.getName(), e.getClass().getSimpleName(), e.getMessage());
			}
			return false; //do not remove dump file
		});
	}
   
	static List<EventTrace> extractPendingMetrics(int seconds, List<EventTrace> traces) {
		if(seconds != 0 && !isEmpty(traces)) {
			var pending = new ArrayList<EventTrace>();
			var now = now();
			for(var it=traces.listIterator(); it.hasNext();) {
				if(it.next() instanceof CompletableMetric o) {
					o.runSynchronizedIfNotComplete(()-> {
						if(seconds > -1 && o.getStart().until(now, SECONDS) > seconds) {
							it.set(o.copy()); //do not put it in pending, will be sent later
							log.trace("pending trace will be sent now : {}", o);
						}
						else { //-1 => do not trace pending
							pending.add(o);
							it.remove();
							log.trace("pending trace will be sent later : {} ", o);
						}
					});
				}
			}
			return pending;
		}
		return emptyList();
	}
	
    public Stream<EventTrace> peek() {
    	return queue.peek();
    }
    
	static boolean isEmpty(List<?> arr) {
		return isNull(arr) || arr.isEmpty();
	}

}
