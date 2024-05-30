package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

	Collection<FtpRequest> getFtpRequests();
	
	Collection<DatabaseRequest> getQueries();

	Collection<RunnableStage> getStages();
	
	AtomicInteger getLock();
	
	default void append(ApiRequest request) {
		getRequests().add(request);
	}

	default void append(FtpRequest request) {
		getFtpRequests().add(request);
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
	
	static <T, E extends Exception> T withActiveSession(SafeSupplier<T,E> cs, Consumer<Session> s) throws E {
		var session = localTrace.get();
		if(isNull(session)) {
			warnNoActiveSession();
			return cs.get();
		}
		try {
			return cs.get();
		}
		catch (Exception e) {
			throw e;
		}
	}
}
