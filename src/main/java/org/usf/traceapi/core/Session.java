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
	
	AtomicInteger getLock();
	
	default void append(ApiRequest request) {
		getRequests().add(request);
	}

	default void append(DatabaseRequest query) {
		getQueries().add(query);
	}
	
	default void append(RunnableStage stage) {
		getStages().add(stage);
	}
	
	default void lock(){
		getLock().incrementAndGet();
	}
	
	default void unlock() {
		getLock().decrementAndGet();
	}
	
	default boolean wasCompleted() {
		var c = getLock().get();
		if(c < 0) {
			log.warn("illegal session lock state={}, {}", c, this);
			return true;
		}
		return c == 0;
	}
	
	static String nextId() {
		return randomUUID().toString();
	}
}
