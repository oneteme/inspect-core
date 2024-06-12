package org.usf.traceapi.core;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.State.DISABLE;
import static org.usf.traceapi.core.State.DISPACH;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public final class ScheduledDispatcher<T> {
	
	final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	
    private final SessionDispatcherProperties properties;
    private final Dispatcher<T> dispatcher;
    private final Predicate<? super T> filter;
    private final List<T> queue;
    @Getter
    private volatile State state = DISPACH;
    private int attempts;
    
    public ScheduledDispatcher(SessionDispatcherProperties properties, Dispatcher<T> dispatcher) {
    	this(properties, dispatcher, null);
    }
    
	public ScheduledDispatcher(SessionDispatcherProperties properties, Dispatcher<T> dispatcher, Predicate<? super T> filter) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.filter = filter;
		this.queue = new ArrayList<>(properties.getBufferSize());
    	this.executor.scheduleWithFixedDelay(this::tryDispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}

	@SuppressWarnings("unchecked")
	public boolean add(T... arr) {
		if(state != DISABLE) { // CACHE | DISPATCH
			doSync(q-> addAll(q, arr));
			log.trace("{} items buffered", queue.size());
			return true;
		}
		log.warn("{} items rejected, dispatcher.state={}", arr.length, state);
		return false;
	}
	
	public void updateState(State state) {
		this.state = state;
	}
	
    private void tryDispatch() {
    	if(state == DISPACH) {
    		dispatch();
    	}
    	else {
    		log.warn("dispatcher.state={}", state);
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

    private void dispatch() {
    	var cs = pop();
        if(!cs.isEmpty()) {
	        log.trace("scheduled dispatching {} items..", cs.size());
	        try {
	        	if(dispatcher.dispatch(++attempts, unmodifiableList(cs))) {
	        		attempts=0;
	        	}
	    	}
	    	catch (Exception e) {// do not throw exception : retry later
	    		log.warn("error while dispatching {} items, attempts={} because : {}", 
	    				cs.size(), attempts, e.getMessage()); //do not log exception stack trace
			}
	        if(attempts > 0) { //exception | !dispatch
	        	doSync(q-> q.addAll(0, cs));
	        }
        }
    }

    public List<T> peek() {
    	return applySync(q-> {
    		if(q.isEmpty()) {
    			return emptyList();
    		}
    		var s = q.stream();
    		if(nonNull(filter)) {
    			s = s.filter(filter);
    		}
    		return s.toList();
    	});
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
 
    public void shutdown() throws InterruptedException {
    	updateState(DISABLE); //stop add items
    	log.info("shutting down scheduler service");
    	try {
    		executor.shutdown(); //cancel future
    		while(!executor.awaitTermination(5, SECONDS)); //wait for last save complete
    	}
    	finally {
    		tryDispatch();
		}
    }
	
	@FunctionalInterface
	public interface Dispatcher<T> {
		
		boolean dispatch(int attempts, List<T> list) throws Exception; //TD return List<T> dispatched sessions  
	}
}
