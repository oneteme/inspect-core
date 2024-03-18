package org.usf.traceapi.core;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toCollection;
import static org.usf.traceapi.core.Helper.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 
 * @author u$f
 *
 */
public final class ScheduledSessionDispatcher {
	
	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	
    private final List<Session> queue;
    private final SessionDispatcherProperties properties;
    private final Dispatcher dispatcher;
    private final Predicate<Session> filter;
    private final AtomicBoolean state = new AtomicBoolean(true);
    private int attempts;
    
    public ScheduledSessionDispatcher(SessionDispatcherProperties properties, Dispatcher dispatcher) {
    	this(properties, null, dispatcher);
    }
    
	public ScheduledSessionDispatcher(SessionDispatcherProperties properties, Predicate<Session> filter, Dispatcher dispatcher) {
		this.queue = new ArrayList<>(properties.getBufferSize());
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.filter = filter;
    	executor.scheduleWithFixedDelay(this::dispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}

	public void add(Session... sessions) {
		sync(q-> addAll(queue, sessions));
		log.trace("{} sessions buffered", queue.size());
	}
	
	public void dispatchState(boolean v) { //on|off
		state.set(v);
	}
	
    private void dispatch() {
    	if(state.get()) {
	    	var cs = peekList();
	        if(!cs.isEmpty()) {
		        log.trace("scheduled dispatching {} sessions..", cs.size());
		        try {
		        	if(dispatcher.dispatch(++attempts, cs)) {
		        		removeAll(cs);
		        		attempts=0;
		        	}
		    	}
		    	catch (Exception e) {
		    		// do not throw exception : retry later
		    		log.warn("error while dispatching {} sessions, attempts={} because : {}", cs.size(), attempts, e.getMessage()); //do not log exception stack trace
		    		if(properties.getBufferMaxSize() > -1 && cs.size() > properties.getBufferMaxSize()) {
		    			//remove exceeding cache sessions (LIFO)
		    			var diff = cs.size() - properties.getBufferMaxSize();
		    			sync(q-> { q.subList(cs.size()-diff, cs.size()).clear(); return null;});
			    		log.warn("{} last sessions have been removed from buffer", diff); 
		    		}
				}
	        }
    	}
    }

    public List<Session> peekList() {
    	return sync(q-> {
    		if(q.isEmpty()) {
    			return emptyList();
    		}
    		var s = queue.stream();
    		if(nonNull(filter)) {
    			s = s.filter(filter);
    		}
    		return s.collect(toCollection(SessionList::new));
    	});
    }

    private void removeAll(List<? extends Session> cs) {
    	sync(q-> {
			if(nonNull(filter)) {
				q.removeAll(cs);
			}
			else { //more efficient
				q.subList(0, cs.size()).clear();
			}
			return null;
    	});
    }

    private <T> T sync(Function<List<Session>, T> queueFn) {
		synchronized(queue){
			return queueFn.apply(queue);
		}
    }
    
    public void shutdown() throws InterruptedException {
    	log.info("shutting down scheduler service");
    	try {
    		executor.shutdown(); //cancel future
    		while(!executor.awaitTermination(5, SECONDS)); //wait for last save complete
    	}
    	finally {
    		dispatch();
		}
    }
    
	//jackson issue : https://github.com/FasterXML/jackson-databind/issues/23
	@SuppressWarnings("serial") 
	static final class SessionList extends ArrayList<Session> {}
	
	@FunctionalInterface
	public interface Dispatcher {
		
		boolean dispatch(int attempts, List<Session> sessions);
	}
}
