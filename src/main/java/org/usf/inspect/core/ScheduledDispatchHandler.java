package org.usf.inspect.core;

import static java.lang.Thread.currentThread;
import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.empty;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.DispatchState.DISPACH;
import static org.usf.inspect.core.Helper.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public final class ScheduledDispatchHandler<T> implements SessionHandler<T> {
	
	final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	
    private final ScheduledDispatchProperties properties;
    private final Dispatcher<T> dispatcher;
    private final Predicate<? super T> filter;
    private final List<T> queue;
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
		this.queue = new ArrayList<>(properties.getBufferSize());
    	this.executor.scheduleWithFixedDelay(this::tryDispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void handle(T o) {
		submit(o);
	}
	
	@SuppressWarnings("unchecked")
	public boolean submit(T... arr) {
		var fail = state == DISABLE || !applySync(q-> { // CACHE | DISPATCH
			var size = q.size();
			var done = false;
			try {
				done = addAll(q, arr);  //false | OutOfMemoryError
			} finally {
				if(done) {
					log.trace("{} new items buffered, queue.size={}", arr.length, q.size());
				} //addAll or nothing
				else if(q.size() > size) { //partial add
					q.subList(size, q.size()).clear();
				}
			}
			return done;
		});
		if(fail) {
			log.warn("{} items rejected, dispatcher.state={}", arr.length, state);
		}
		return !fail;
	}
	
	public void updateState(DispatchState state) throws InterruptedException {
		if(this.state != state) {
			this.state = state;
		}
		if(state == DISABLE) { //wait for last dispatch complete
    		while(!executor.awaitTermination(10, SECONDS));
		}
		log.info("dispatcher.state={}", state);
	}
	
    private void tryDispatch() {
    	if(state == DISPACH) {
    		dispatch(false);
    	}
    	else {
    		doSync(q-> log.warn("dispatcher.state={}, queue.size={}", state, q.size()));
    	}
    	if(properties.getBufferMaxSize() > -1 && (state != DISPACH || attempts > 0)) { // !DISPACH | dispatch=fail
        	doSync(q-> {
        		if(q.size() > properties.getBufferMaxSize()) {
        			var diff = q.size() - properties.getBufferMaxSize();
        			q.subList(properties.getBufferMaxSize(), q.size()).clear(); //remove exceeding cache sessions (LIFO)
    	    		log.warn("{} last items have been removed from buffer", diff); 
        		}
    		});
    	}
    }

    private void dispatch(boolean complete) {
    	var cs = pop();
        if(!cs.isEmpty()) {
	        log.trace("scheduled dispatching {} items..", cs.size());
	        try {
	        	if(dispatcher.dispatch(complete, ++attempts, unmodifiableList(cs))) {
	        		if(attempts > 1) { //!first attempt
	        			log.info("{} items dispatched, after {} attempts", cs.size(), attempts);
	        		}
	        		attempts=0;
	        	}
	    	}
	    	catch (Exception e) {// do not throw exception : retry later
	    		log.warn("error while dispatching {} items, attempts={} because :[{}] {}", 
	    				cs.size(), attempts, e.getClass().getSimpleName(), e.getMessage()); //do not log exception stack trace
			}
	        if(attempts > 0) { //exception | !dispatch
	        	doSync(q-> {
	        		var size = q.size();
	        		var done = false;
	        		try {
	        			done = q.addAll(0, cs);  //false | OutOfMemoryError
	        		}
	        		finally {
						if(!done) {
		    	    		log.warn("{} items have been lost from buffer", size + cs.size() - q.size());
						}
					}
	        	});
	        }
        }
    }

    public Stream<T> peek() {
    	if(state == DISABLE) { //deny buffer peek if dispatcher active
        	return applySync(q-> {
        		if(q.isEmpty()) {
        			return empty();
        		}
        		var s = q.stream();
        		return isNull(filter) ? s : s.filter(filter);
        	});
    	}
    	throw new IllegalStateException("dispatcher.state=" + state);
    }
    
    List<T> pop() {
    	return applySync(q-> {
    		if(q.isEmpty()) {
    			return emptyList();
    		}
    		if(isNull(filter)) {
    			var c = new ArrayList<>(q);
    			q.clear();
    			return c;
    		}
    		var c = new ArrayList<T>(q.size());
    		for(var it=q.iterator(); it.hasNext();) {
    			var o = it.next();
    			if(filter.test(o)) {
    				c.add(o);
    				it.remove();
    			}
    		}
    		if(q.size() > c.size()) {
    			log.info("{}/{} sessions are not yet completed", q.size()-c.size(), q.size());
    		}
    		return c;
    	});
    }
    
    private void doSync(Consumer<List<T>> cons) {
    	synchronized(queue){
			cons.accept(queue);
		}
    }

    private <R> R applySync(Function<List<T>, R> fn) {
		synchronized(queue){
			return fn.apply(queue);
		}
    }
 
    @Override
    public void complete() {
    	var stt = state;
    	log.info("shutting down scheduler service");
    	try {
    		executor.shutdown(); //cancel future
    		updateState(DISABLE); //stop add items
    		if(stt == DISPACH) {
    			dispatch(true); //complete signal
    		}
    	}
    	catch (InterruptedException e) {
    		log.error("awaitDispatching interrupted", e);
    		currentThread().interrupt();
		}
    	finally {
    		if(!queue.isEmpty()) { //!dispatch || dispatch=fail + incomplete session
    			log.warn("{} items aborted, dispatcher.state={}", queue.size(), stt); // safe queue access
    		}
		}
    }
    
	@FunctionalInterface
	public interface Dispatcher<T> {
		
		boolean dispatch(boolean complete, int attempts, List<T> list) throws Exception; //TD return List<T> dispatched sessions
	}
}
