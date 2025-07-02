package org.usf.inspect.core;

import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Helper.log;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * @author u$f
 *
 */
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "@type")
public interface Session extends Metric {
	
	String getId();

	void lock(); //must be called before session end
	
	void unlock();
	
	boolean isCompleted(); //async task
	
	static String nextId() {
		return randomUUID().toString();
	}
	
	public interface Task {
		
		void run(Session session) throws Exception;
		
		default boolean runSilently(Session session) {
			try {
				run(session);
				return true;
			} catch (Throwable e) {
				log.warn("cannot execute task on session {}", this);
			}
			return false;
		}
	}
}
