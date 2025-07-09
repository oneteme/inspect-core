package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Stream.empty;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPATCH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class ScheduledDispatchHandler implements TraceHandler<Traceable> {
	
	private static final byte UNLIMITED = 0;
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(r->{
		var thr = new Thread(r, "inspect-dispatcher");
 		thr.setDaemon(true);
 		thr.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception", e));
		return thr;
	});
	
    private final SchedulingProperties properties;
    private final Dispatcher<Traceable> dispatcher;
    private final ThreadSafeQueue<Traceable> queue;
    @Getter
    private volatile DispatchState state;
    private int attempts;

    public ScheduledDispatchHandler(SchedulingProperties properties, Dispatcher<Traceable> dispatcher) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.state = DISPATCH; //default state
		this.queue = new ThreadSafeQueue<>();
    	this.executor.scheduleWithFixedDelay(this::tryDispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}
	
	@Override
	public void handle(Traceable o) {
		if(state != DISABLE) {
			queue.add(o);
		}
		else {
			log.warn("rejected 1 new items, current dispatcher state: {}", 1, state);
		}
	}
	
	public boolean submit(List<Traceable> arr) {
		if(state != DISABLE) {
			queue.addAll(arr);
			return true;
		}
		log.warn("rejected {} new items, current dispatcher state: {}", arr.size(), state);
		return false;
	}
	
	public void updateState(DispatchState state) {
		if(this.state != state) {
			synchronized (executor) { //wait for dispatch end
				this.state = state;
			}
			log.info("dispatcher state was changed to {}", state);
		}
	}
	
    private void tryDispatch() {
		synchronized (executor) {
	    	if(state == DISPATCH) {
    			dispatch(false);
    		}
	    	else {
	    		log.warn("cannot dispatch items as the dispatcher state is {}, current queue size: {}", state, queue.size());
	    	}
	    	if(properties.getQueueCapacity() > UNLIMITED && (state != DISPATCH || attempts > 0)) { // !DISPACH | dispatch=fail
	    		queue.removeRange(properties.getQueueCapacity()); //remove exceeding cache sessions (LIFO)
	    	}
    	}
    }

    private void dispatch(boolean complete) {
    	var cs = queue.pop();
        log.trace("scheduled dispatch of {} items...", cs.size());
        try {
        	var modifiable = new ArrayList<>(cs);
        	var pending = extractPendingMetrics(properties.getDispatchDelayIfPending(), modifiable);
        	if(dispatcher.dispatch(complete, ++attempts, pending.size(), modifiable)) { 
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

	static List<Traceable> extractPendingMetrics(int seconds, List<Traceable> traces) {
		if(seconds != 0 && !isEmpty(traces)) {
			var pending = new ArrayList<Traceable>();
			var now = now();
			for(var it=traces.listIterator(); it.hasNext();) {
				if(it.next() instanceof CompletableMetric o) {
					o.runIfPending(()-> {
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
    
    public Stream<Traceable> peek() {
    	return queue.peek();
    }
    
    @Override
    public void complete() {
    	var stt = state;
    	log.info("shutting down the scheduler service...");
    	try {
    		executor.shutdown(); //cancel schedule
    		updateState(DISABLE); //stop add items after waiting for last dispatch
    		if(stt == DISPATCH) {
				dispatch(true); //complete signal
    		}
    	}
    	finally {
    		if(queue.size() > 0) { //!dispatch || dispatch=fail + incomplete session
    			log.warn("{} items were aborted, dispatcher state: {}", queue.size(), stt);
    		}
		}
    }
    
	private final class ThreadSafeQueue<T> {
	
		private final Object mutex = new Object();
	    private LinkedHashSet<T> queue; //guarantees order and uniqueness of items, no duplicates (force updates)
	
		public ThreadSafeQueue() {
			this.queue = new LinkedHashSet<>();
		}
		
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