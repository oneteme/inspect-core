package org.usf.traceapi.core;

import static java.util.UUID.randomUUID;
import static org.usf.traceapi.core.Helper.log;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

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
	
	String getId(); //UUID
	
	void setId(String id); //used in server side
	
	Collection<ApiRequest> getRequests();
	
	Collection<DatabaseRequest> getQueries();

	Collection<RunnableStage> getStages();
	
	void append(ApiRequest request); // sub requests

	void append(DatabaseRequest query); // sub queries
	
	void append(RunnableStage stage); // sub stages
	
	AtomicInteger getLock();
	
	default void lock(){
		getLock().incrementAndGet();
	}
	
	default void unlock() {
		if(getLock().get() > 0) {
			getLock().decrementAndGet();
		}
		else {
			log.warn("no more lock");
		}
	}
	
	default boolean wasCompleted() {
		return getLock().get() == 0;
	}
	
	static String nextId() {
		return randomUUID().toString();
	}
}
