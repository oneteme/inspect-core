package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Helper.log;

import java.util.List;
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
	
	List<RestRequest> getRestRequests();
	
	List<DatabaseRequest> getDatabaseRequests();

	List<LocalRequest> getLocalRequests();
	
	List<FtpRequest> getFtpRequests();

	List<MailRequest> getMailRequests();

	List<NamingRequest> getLdapRequests();
	
	AtomicInteger getLock();
	
	default boolean append(SessionStage stage) {
		if(stage instanceof RestRequest req) {
			return getRestRequests().add(req);
		}
		if(stage instanceof DatabaseRequest req) {
			return getDatabaseRequests().add(req);
		}
		if(stage instanceof FtpRequest req) {
			return getFtpRequests().add(req);
		}
		if(stage instanceof MailRequest req) {
			return getMailRequests().add(req);
		}
		if(stage instanceof NamingRequest req) {
			return getLdapRequests().add(req);
		}
		if(stage instanceof LocalRequest req) {
			return getLocalRequests().add(req);
		}
		log.warn("unsupported session stage {}", stage);
		return false;
	}
	
	default void lock(){ //must be called before session end
		getLock().incrementAndGet();
	}
	
	default void unlock() {
		getLock().decrementAndGet();
	}
	
	default boolean completed() {
		var c = getLock().get();
		if(c < 0) {
			log.warn("illegal session lock state={}, {}", c, this);
			return true;
		}
		return nonNull(getEnd()) && c == 0;
	}
	
	static String nextId() {
		return randomUUID().toString();
	}
}
