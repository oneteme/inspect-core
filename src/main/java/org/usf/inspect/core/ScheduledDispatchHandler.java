package org.usf.inspect.core;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Stream.empty;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPACH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class ScheduledDispatchHandler<T> implements SessionHandler<T> {
	
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor(r->{
		var t = new Thread(r);
		t.setName("inspect-scheduler");
		t.setDaemon(true);
		return t;
	});
	
    private final ScheduledDispatchProperties properties;
    private final Dispatcher<T> dispatcher;
    private final SafeQueue queue;
    private final Predicate<? super T> filter;
    @Getter
    private volatile DispatchState state;
    private int attempts;

    public ScheduledDispatchHandler(ScheduledDispatchProperties properties, Dispatcher<T> dispatcher) {
    	this(properties, dispatcher, null);
    }
    
	public ScheduledDispatchHandler(ScheduledDispatchProperties properties, Dispatcher<T> dispatcher, Predicate<? super T> filter) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.filter = filter;
		this.state = properties.getState();
		this.queue = new SafeQueue(properties.getBufferSize());
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
	    	if(state == DISPACH) {
    			dispatch(false);
    		}
	    	else {
	    		log.warn("cannot dispatch items as the dispatcher state is {}, current queue size: {}", state, queue.size());
	    	}
	    	if(properties.getBufferMaxSize() > -1 && (state != DISPACH || attempts > 0)) { // !DISPACH | dispatch=fail
	    		queue.removeRange(properties.getBufferMaxSize()); //remove exceeding cache sessions (LIFO)
	    	}
    	}
    }

    private void dispatch(boolean complete) {
    	var cs = queue.pop(filter);
    	var pg = queue.size(); // ~pending + delta
        if(!cs.isEmpty() || complete) {
	        log.trace("scheduled dispatch of {} items...", cs.size());
	        try {
	        	if(dispatcher.dispatch(complete, ++attempts, unmodifiableList(cs), pg)) {
	        		if(attempts > 1) { //more than one attempt
	        			log.info("successfully dispatched {} items after {} attempts", cs.size(), attempts);
	        		}
	        		attempts=0;
	        	}
	    	}
	    	catch (Exception e) {// do not throw exception : retry later
	    		log.warn("failed to dispatch {} items after {} attempts, cause: [{}] {}", 
	    				cs.size(), attempts, e.getClass().getSimpleName(), e.getMessage()); //do not log exception stack trace
			}
	    	catch (Throwable e) {
	    		log.error("failed to dispatch {} items after {} attempts", cs.size(), attempts, e);
    			throw e;
			}
	        finally {
		        if(attempts > 0) { //exception | !dispatch
		        	queue.addAll(0, cs); //back to queue
		        }
			}
        }
    }
    
    public Stream<T> peek() {
    	return queue.peek(filter);
    }
    
    @Override
    public void complete() {
    	var stt = state;
    	log.info("shutting down the scheduler service...");
    	try {
    		executor.shutdown(); //cancel schedule
    		updateState(DISABLE); //stop add items  wait for last dispatch
    		if(stt == DISPACH) {
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
		
		boolean dispatch(boolean complete, int attempts, List<T> list, int pending) throws Exception; //TD return List<T> dispatched sessions
	}
	
	private final class SafeQueue {
	
	    private final List<T> queue;
	
		public SafeQueue(int initialSize) {
			this.queue = new ArrayList<>(initialSize);
		}
		
		public void addAll(T[] arr){
	    	addAll(asList(arr)); // see Arrays$ArrayList
		}
		
		public void addAll(Collection<T> arr){
	    	synchronized(queue){
				queue.addAll(arr);
				logAddedItems(arr.size(), queue.size());
			}
		}
		
		public void addAll(int index, Collection<T> arr){
	    	synchronized(queue){
				queue.addAll(index, arr);
				logAddedItems(arr.size(), queue.size());
			}
		}
	
	    public Stream<T> peek(Predicate<? super T> filter) {
	    	synchronized(queue){
	    		if(queue.isEmpty()) {
	    			return empty();
	    		}
	    		var s = queue.stream();
	    		return isNull(filter) ? s : s.filter(filter);
	    	}
	    }
	    
	    public List<T> pop(Predicate<? super T> filter) {
	    	synchronized(queue){
	    		if(queue.isEmpty()) {
	    			return emptyList();
	    		}
	    		if(isNull(filter)) {
	    			var c = new ArrayList<>(queue);
	    			queue.clear(); //remove items, but preserve capacity
	    			return c;
	    		}
	    		var c = new ArrayList<T>(queue.size());
	    		for(var it=queue.iterator(); it.hasNext();) {
	    			var o = it.next();
	    			if(filter.test(o)) {
	    				c.add(o);
	    				it.remove();
	    			}
	    		}
	    		return c;
	    	}
	    }
		
		public int size() {
			synchronized (queue) {
				return queue.size();
			}
		}
		
		public void removeRange(int fromIndex) {
			synchronized (queue) {
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