package org.usf.traceapi.core;

import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toCollection;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.State.DISABLE;
import static org.usf.traceapi.core.State.DISPACH;

import java.util.ArrayList;
import java.util.Collection;
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
public final class ScheduledSessionDispatcher {
	
	final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	
    private final List<Session> queue;
    private final SessionDispatcherProperties properties;
    private final Dispatcher dispatcher;
    private final Predicate<Session> filter;
    @Getter
    private volatile State state = DISPACH;
    private int attempts;
    
    public ScheduledSessionDispatcher(SessionDispatcherProperties properties, Dispatcher dispatcher) {
    	this(properties, null, dispatcher);
    }
    
	public ScheduledSessionDispatcher(SessionDispatcherProperties properties, Predicate<Session> filter, Dispatcher dispatcher) {
		this.queue = new ArrayList<>(properties.getBufferSize());
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.filter = filter;
    	executor.scheduleWithFixedDelay(this::tryDispatch, properties.getDelay(), properties.getDelay(), properties.getUnit());
	}

	public boolean add(Session... sessions) {
		if(state != DISABLE && applySync(q-> addAll(q, sessions))) { // CACHE | DISPATCH
			log.trace("{} sessions buffered", queue.size());
			return true;
		}
    	else {
    		log.warn("{} sessions rejected, dispatcher.state={}", sessions.length, state);
    		return false;
    	}
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
    }

    private void dispatch() {
    	var cs = popSessions();
        if(!cs.isEmpty()) {
	        log.trace("scheduled dispatching {} sessions..", cs.size());
	        try {
	        	if(dispatcher.dispatch(++attempts, cs)) {
	        		attempts=0;
	        	}
	    	}
	    	catch (Exception e) {// do not throw exception : retry later
	    		log.warn("error while dispatching {} sessions, attempts={} because : {}", cs.size(), attempts, e.getMessage()); //do not log exception stack trace
				doSync(q-> q.addAll(0, cs));
			}
	        if(attempts > 0) { //exception | !dispatch
	        	doSync(q-> {
		    		if(properties.getBufferMaxSize() > -1 && q.size() > properties.getBufferMaxSize()) {
		    			var diff = q.size() - properties.getBufferMaxSize();
		    			q.subList(properties.getBufferMaxSize(), cs.size()).clear();  //remove exceeding cache sessions (LIFO)
			    		log.warn("{} last sessions have been removed from buffer", diff); 
		    		}
				});
	        }
        }
    }

    public List<Session> peekSessions() {
    	return applySync(q-> {
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
    
    public List<Session> popSessions() {
    	return applySync(q-> {
    		if(q.isEmpty()) {
    			return emptyList();
    		}
    		if(isNull(filter)) {
    			var c = new SessionList(q);
    			q.clear();
    			return c;
    		}
    		var c = new SessionList(q.size());
    		for(var it=q.iterator(); it.hasNext();) {
    			var s = it.next();
    			if(filter.test(s)) {
    				c.add(s);
    				it.remove();
    			}
    		}
    		return c;
    	});
    }

    private void doSync(Consumer<List<Session>> cons) {
    	synchronized(queue){
			cons.accept(queue);
		}
    }

    private <T> T applySync(Function<List<Session>, T> fn) {
		synchronized(queue){
			return fn.apply(queue);
		}
    }
 
    public void shutdown() throws InterruptedException {
    	updateState(DISABLE); //stop add Sessions
    	log.info("shutting down scheduler service");
    	try {
    		executor.shutdown(); //cancel future
    		while(!executor.awaitTermination(5, SECONDS)); //wait for last save complete
    	}
    	finally {
    		dispatch();
		}
    }
    
	//jackson issue @JsonTypeInfo : https://github.com/FasterXML/jackson-databind/issues/23
	@SuppressWarnings("serial") 
	static final class SessionList extends ArrayList<Session> {

		public SessionList() {
			super();
		}
		
		public SessionList(int initialCapacity) {
			super(initialCapacity);
		}

		public SessionList(Collection<? extends Session> c) {
			super(c);
		}
	}
	
	@FunctionalInterface
	public interface Dispatcher {
		
		boolean dispatch(int attempts, List<Session> sessions);
	}
}
