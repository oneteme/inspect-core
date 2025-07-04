package org.usf.inspect.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Stream.empty;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPTACH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public final class ScheduledDispatchHandler<T> implements TraceHandler<T> {
	
	private static final byte UNLIMITED = 1;
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(r->{
		var thr = new Thread(r, "inspect-dispatcher");
 		thr.setDaemon(true);
 		thr.setUncaughtExceptionHandler((t,e)-> log.error("uncaught exception", e));
		return thr;
	});
	
    private final ScheduledDispatchProperties properties;
    private final Dispatcher<T> dispatcher;
    private final ThreadSafeQueue queue;
    @Getter
    private volatile DispatchState state;
    private int attempts;

    public ScheduledDispatchHandler(ScheduledDispatchProperties properties, Dispatcher<T> dispatcher) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.state = properties.getState();
		this.queue = new ThreadSafeQueue(properties.getBufferSize());
    	this.executor.scheduleWithFixedDelay(this::tryDispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void handle(T o) {
		submit(o);
	}
	
	@SuppressWarnings("unchecked")
	public boolean submit(T... arr) {
		if(state != DISABLE) {
			queue.addAll(arr);
			return true;
		}
		log.warn("rejected {} new items, current dispatcher state: {}", arr.length, state);
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
	    	if(state == DISPTACH) {
    			dispatch(false);
    		}
	    	else {
	    		log.warn("cannot dispatch items as the dispatcher state is {}, current queue size: {}", state, queue.size());
	    	}
	    	if(properties.getBufferMaxSize() > UNLIMITED && (state != DISPTACH || attempts > 0)) { // !DISPACH | dispatch=fail
	    		queue.removeRange(properties.getBufferMaxSize()); //remove exceeding cache sessions (LIFO)
	    	}
    	}
    }

    private void dispatch(boolean complete) {
    	var cs = queue.pop();
        if(!cs.isEmpty() || complete) {
	        log.trace("scheduled dispatch of {} items...", cs.size());
	        try {
	        	 cs = dispatcher.dispatch(complete, ++attempts, cs);
        		if(attempts > 1) { //more than one attempt
        			log.info("successfully dispatched {} items after {} attempts", cs.size(), attempts);
        		}
        		attempts=0;
	    	}
	    	catch (Exception e) {// do not throw exception : retry later
	    		log.warn("failed to dispatch {} items after {} attempts, cause: [{}] {}", 
	    				cs.size(), attempts, e.getClass().getSimpleName(), e.getMessage()); //do not log exception stack trace
			}
	        catch (OutOfMemoryError e) {
				log.error("out of memory error while dispatching {} items, those will be aborted", cs.size());
				cs = emptyList(); //do not add items back to the queue, may release memory
				attempts = 0;
	        }
	        finally { //TODO file after 100 attempts or 90% max memory
	        	if(!cs.isEmpty()) {
		        	queue.addAll(0, cs); //go back to the queue
	        	}
	        }
        }
    }
    
    public Stream<T> peek() {
    	return queue.peek();
    }
    
    @Override
    public void complete() {
    	var stt = state;
    	log.info("shutting down the scheduler service...");
    	try {
    		executor.shutdown(); //cancel schedule
    		updateState(DISABLE); //stop add items after waiting for last dispatch
    		if(stt == DISPTACH) {
				dispatch(true); //complete signal
    		}
    	}
    	finally {
    		if(queue.size() > 0) { //!dispatch || dispatch=fail + incomplete session
    			log.warn("{} items were aborted, dispatcher state: {}", queue.size(), stt);
    		}
		}
    }
    
	@FunctionalInterface
	public interface Dispatcher<T> {
		
		List<T> dispatch(boolean complete, int attemps, List<T> metrics); //return undispatched items
	}
	
	private final class ThreadSafeQueue {
	
		private Object mutex = new Object();
	    private final int initialSize;
	    private List<T> queue;
	
		public ThreadSafeQueue(int initialSize) {
			this.queue = new ArrayList<>(initialSize);
			this.initialSize = initialSize;
		}
		
		public void addAll(T[] arr){// see Arrays$ArrayList
	    	addAll(asList(arr)); // see Arrays$ArrayList
	    	synchronized(mutex){
				Collections.addAll(queue, arr); 
				logAddedItems(arr.length, queue.size());
			}
		}
		
		public void addAll(Collection<T> arr){
	    	synchronized(mutex){
				queue.addAll(arr);
				logAddedItems(arr.size(), queue.size());
			}
		}
		
		public void addAll(int index, Collection<T> arr){
	    	synchronized(mutex){
				queue.addAll(index, arr);
				logAddedItems(arr.size(), queue.size());
			}
		}
	
	    public Stream<T> peek() {
	    	synchronized(mutex){
	    		return queue.isEmpty() ? empty() : queue.stream();
	    	}
	    }
	    
	    public List<T> pop() {
	    	synchronized(mutex){
	    		if(queue.isEmpty()) {
	    			return emptyList();
	    		}
	    		var res = queue;
    			queue = new ArrayList<>(initialSize); //reset queue, may release memory (!clear)
    			return res;
	    	}
	    }
		
		public int size() {
			synchronized (mutex) {
				return queue.size();
			}
		}
		
		public void removeRange(int fromIndex) {
			synchronized (mutex) {
				if(fromIndex < queue.size()) {
					var n = queue.size() - fromIndex;
					queue.subList(fromIndex, queue.size()).clear();
					log.warn("removed {} most recent items from the queue, current queue size: {}", n, queue.size());
				}
			}
		}
	    
	    static void logAddedItems(int nItems, int queueSize) {
			log.trace("added {} new items to the queue, current queue size: {}", nItems, queueSize);
	    }
	}	
}