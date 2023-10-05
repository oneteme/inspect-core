package org.usf.traceapi.core;

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toCollection;
import static org.usf.traceapi.core.Helper.log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

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
    	executor.scheduleWithFixedDelay(this::sendAll, prop.getDelay(), prop.getDelay(), prop.getUnit());
	}
	
	@Override
	public void handle(Session session) {
		synchronized(sessionQueue){
			sessionQueue.add(session);
		}
		log.debug("new session added to the queue : {} session(s)", sessionQueue.size());
	}
	
    private void sendAll() {
    	List<Session> cs;
		synchronized(sessionQueue){
			cs = sessionQueue.isEmpty() 
    			? emptyList()
    			: sessionQueue.stream() // stream must be manually synched by user! see synchronizedCollection
    			.filter(Session::wasCompleted)
    			.collect(toCollection(SessionList::new)); 
		}
        if(!cs.isEmpty()) {
	        log.info("scheduled data queue sending.. : {} session(s)", cs.size());
	        try {
	        	template.put(properties.getUrl(), cs);
	    		synchronized(sessionQueue){
	    			sessionQueue.removeAll(cs);
	    		}
	    	}
	    	catch (Exception e) {
	    		log.error("error while sending sessions", e);
	    		if(cs.size() > properties.getMaxCachedSession()) {
	    			synchronized(sessionQueue){ //remove fist n sessions
	    				sessionQueue.subList(0, cs.size() - properties.getMaxCachedSession()).clear();
	    			}
	    		}
	    		// do not throw exception : retry later
			}
        }
    }
    
	//Jackson issue : https://github.com/FasterXML/jackson-databind/issues/23
	@SuppressWarnings("serial") 
	static final class SessionList extends ArrayList<Session> {}
	
}
