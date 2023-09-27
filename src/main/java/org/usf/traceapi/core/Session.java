package org.usf.traceapi.core;

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
	
	String getName();
	
	String getUser();
	
	ApplicationInfo getApplication();
	
	Collection<ApiRequest> getRequests();
	
	Collection<DatabaseRequest> getQueries();
	
	void append(ApiRequest request); // sub requests

	void append(DatabaseRequest query); // sub queries
	
	void append(RunnableStage stage); // sub stages
	
	AtomicInteger getLock();
	
	default void lock(){
		getLock().incrementAndGet();
	}
	
	default void unlock() {
		getLock().decrementAndGet();
	}
	
	default boolean wasCompleted() {
		return getLock().intValue() == 0;
	}
}
