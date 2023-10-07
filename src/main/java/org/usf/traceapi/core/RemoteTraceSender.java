package org.usf.traceapi.core;

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toCollection;
import static org.usf.traceapi.core.Helper.log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import org.springframework.web.client.RestTemplate;

/**
 * 
 * @author u$f
 *
 */
public final class RemoteTraceSender implements TraceHandler {
	
	static final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
	
    private final LinkedList<Session> sessionQueue = new LinkedList<>();
	private final TraceConfigurationProperties properties;
	private final RestTemplate template;

	public RemoteTraceSender(TraceConfigurationProperties properties) {
		this(properties, new RestTemplate());
	}
	
	public RemoteTraceSender(TraceConfigurationProperties prop, RestTemplate template) {
		this.properties = prop;
		this.template = template;
    	executor.scheduleWithFixedDelay(this::sendCompleted, prop.getDelay(), prop.getDelay(), prop.getUnit());
	}
	
	@Override
	public void handle(Session session) {
		safeQueue(q-> q.add(session));
		log.debug("new session added to the queue : {} session(s)", sessionQueue.size());
	}
	
    private void sendCompleted() {
    	var cs = safeQueue(q-> q.isEmpty() 
    			? emptyList()
    			: sessionQueue.stream()
    			.filter(Session::wasCompleted)
    			.collect(toCollection(SessionList::new)));
        if(!cs.isEmpty()) {
	        log.debug("scheduled data queue sending.. : {} session(s)", cs.size());
	        try {
	        	template.put(properties.getUrl(), cs);
	        	safeQueue(q-> q.removeAll(cs));
	    	}
	    	catch (Exception e) {
	    		log.warn("error while sending sessions", e);
	    		if(cs.size() > properties.getWaitListSize()) {
	    			//remove exceeding cache sessions (FIFO)
	    			safeQueue(q-> q.removeAll(cs.subList(0, cs.size() - properties.getWaitListSize())));
	    		}
	    		// do not throw exception : retry later
			}
        }
    }
    
    private <T> T safeQueue(Function<LinkedList<Session>, T> queueFn) {
		synchronized(sessionQueue){
			return queueFn.apply(sessionQueue);
		}
	}
    
	//jackson issue : https://github.com/FasterXML/jackson-databind/issues/23
	@SuppressWarnings("serial") 
	static final class SessionList extends ArrayList<Session> {}
	
}
