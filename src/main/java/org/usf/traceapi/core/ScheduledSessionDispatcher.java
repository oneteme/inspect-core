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
import java.util.function.BiConsumer;
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
    private final BiConsumer<Integer, List<Session>> consumer;
    private final Predicate<Session> filter;
    private final AtomicBoolean dispatch = new AtomicBoolean(true);
    private int attempts;
    
    public ScheduledSessionDispatcher(SessionDispatcherProperties properties, BiConsumer<Integer, List<Session>> consumer) {
    	this(properties, consumer, null);
    }
    
	public ScheduledSessionDispatcher(SessionDispatcherProperties properties, BiConsumer<Integer, List<Session>> consumer, Predicate<Session> filter) {
		this.queue = new ArrayList<>(properties.getBufferSize());
		this.properties = properties;
		this.consumer = consumer;
		this.filter = filter;
    	executor.scheduleWithFixedDelay(this::dispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}

	public void add(Session... sessions) {
		sync(q-> addAll(queue, sessions));
		log.trace("new sessions added to the queue : {} session(s)", queue.size());
	}
	
	public void dispatch(boolean v) {
		dispatch.set(v);
	}
	
    private void dispatch() {
    	if(dispatch.get()) {
	    	var cs = peekList();
	        if(!cs.isEmpty()) {
		        log.trace("scheduled data queue dispatching.. : {} session(s), attempts={}", cs.size(), ++attempts);
		        try {
		        	consumer.accept(attempts, cs);
		        	removeAll(cs);
		        	attempts=0;
		    	}
		    	catch (Exception e) {
		    		// do not throw exception : retry later
		    		log.warn("error while dispatching {} sessions {}", cs.size(), e); //do not log exception stack trace
		    		if(properties.getBufferMaxSize() > -1 && cs.size() > properties.getBufferMaxSize()) {
		    			//remove exceeding cache sessions (LIFO)
		    			sync(q-> { q.subList(cs.size() - properties.getBufferMaxSize(), cs.size()).clear(); return null;});
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
}
