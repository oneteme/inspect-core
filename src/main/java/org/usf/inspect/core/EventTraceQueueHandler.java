package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Stream.empty;
import static org.usf.inspect.core.DispatchState.DISPATCH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EventTraceQueueHandler implements EventHandler<EventTrace>, EventListener<DispatchState> {
	
	private final TracingProperties prop;
    private final EventTraceDispatcher<EventTrace> sender;
    private final ThreadSafeQueue<EventTrace> queue = new ThreadSafeQueue<>();
    private int attempts;
	
	@Override
	public void handle(EventTrace trace) throws Exception {
		queue.add(trace);
	}
	
	@Override
	public void onEvent(DispatchState state, boolean complete) throws Exception {
    	if(state == DISPATCH) {
			dispatch(complete);
		}
    	else {
    		log.warn("cannot emit items as the dispatcher state is {}, current queue size: {}", state, queue.size());
    	}
    	if(prop.getQueueCapacity() > 0 && (state != DISPATCH || attempts > 0)) { // !DISPACH | dispatch=fail
    		queue.removeRange(prop.getQueueCapacity()); //remove exceeding cache sessions (LIFO)
    	}
    }
	
   void dispatch(boolean complete) {
    	var cs = queue.pop();
        log.trace("dispatching {} items, current queue size: {}", cs.size(), queue.size());
        try {
        	var modifiable = new ArrayList<>(cs);
        	var pending = extractPendingMetrics(prop.getDelayIfPending(), modifiable);
        	if(sender.dispatch(complete, ++attempts, pending.size(), modifiable)) { 
        		if(attempts > 1) { //more than one attempt
        			log.info("successfully dispatched {} items after {} attempts", cs.size(), attempts);
        		}
        		attempts=0;
        		cs = pending; //keep pending traces for next dispatch
        	}
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
    
	final class ThreadSafeQueue<T> {
	
		private final Object mutex = new Object();
	    private LinkedHashSet<T> queue = new LinkedHashSet<>(); //guarantees order and uniqueness of items, no duplicates (force updates)
	
		/**
		 * Adds an item to the queue, overwriting existing items (more recent).
		 */
		public void add(T o) {
	    	synchronized(mutex){
				queue.add(o);
				logAddedItems(1, queue.size());
	    	}
		}
		
		/**
		 * Adds all items to the queue, overwriting existing items (more recent).
		 */
		public void addAll(Collection<T> arr){
	    	synchronized(mutex){
    			queue.addAll(arr); //add or overwrite items (update)
				logAddedItems(arr.size(), queue.size());
			}
		}
		
		/**
		 * Prepends items to the queue, preserving their order.
		 * If an item already exists in the queue, the existing (more recent) version is kept and the one from {@code arr} is ignored.
		 */
		public void requeueAll(Collection<T> arr){
	    	synchronized(mutex){
	    		var set = new LinkedHashSet<>(arr);
	    		set.addAll(queue); //add or overwrite items (update)
	    		queue = set;
				logAddedItems(arr.size(), queue.size());
			}
		}
		
	    public Stream<T> peek() {
	    	synchronized(mutex){
	    		return queue.isEmpty() ? empty() : queue.stream();
	    	}
	    }
	    
	    /**
	     * Pops all items from the queue, clearing it.
	     */
	    public Collection<T> pop() {
	    	synchronized(mutex){
	    		if(queue.isEmpty()) {
	    			return emptyList();
	    		}
	    		var res = queue;
    			queue = new LinkedHashSet<>(); //reset queue, may release memory (do not use clear())
    			return res;
	    	}
	    }
		
		public int size() {
			synchronized (mutex) {
				return queue.size();
			}
		}
		
		@Deprecated
		public void removeRange(int fromIndex) { //TODO (java21) change this => dump file
			synchronized (mutex) {
				if(fromIndex < queue.size()) {
					var n = queue.size() - fromIndex;
					while(queue.size() > fromIndex) {
						queue.removeLast();
					}
					log.warn("removed {} most recent items from the queue, current queue size: {}", n, queue.size());
				}
			}
		}
	    
	    static void logAddedItems(int nItems, int queueSize) {
			log.trace("{} items added or requeued to the queue (may overwrite existing), current queue size: {}", nItems, queueSize);
	    }
	}

	static boolean isEmpty(List<?> arr) {
		return isNull(arr) || arr.isEmpty();
	}
}
