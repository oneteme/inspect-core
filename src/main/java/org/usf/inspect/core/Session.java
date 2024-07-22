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
	
	default void append(SessionStage stage) {
		if(stage instanceof RestRequest req) {
			getRestRequests().add(req);
		}
		else if(stage instanceof DatabaseRequest req) {
			getDatabaseRequests().add(req);
		}
		else if(stage instanceof FtpRequest req) {
			getFtpRequests().add(req);
		}
		else if(stage instanceof MailRequest req) {
			getMailRequests().add(req);
		}
		else if(stage instanceof NamingRequest req) {
			getLdapRequests().add(req);
		}
		else if(stage instanceof LocalRequest req) {
			getLocalRequests().add(req);
		}
		else {
			log.warn("unsupported session stage {}", stage);
		}
	}
	
	default void lock(){
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
