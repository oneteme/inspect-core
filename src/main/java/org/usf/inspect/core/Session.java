package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.localTrace;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.warnNoActiveSession;
import static org.usf.inspect.core.StageTracker.call;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.core.SafeCallable.SafeRunnable;
import org.usf.inspect.core.StageTracker.StageConsumer;

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
	
	List<RestRequest> getRestRequests();	  // rename to getApiRequests
	
	List<DatabaseRequest> getDatabaseRequests(); //rename to getDatabaseRequests

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
		return c == 0;
	}
	
	static String nextId() {
		return randomUUID().toString();
	}
	
	static <E extends Throwable> void trackRunnable(String name, SafeRunnable<E> fn) throws E {
		trackCallble(name, fn);
	}
	
	static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
		return call(fn, runnableStageAppender(name, localTrace.get()));
	}

	static StageConsumer<Object> runnableStageAppender(Session session) {
		return runnableStageAppender(null, session);
	}
	
	static StageConsumer<Object> runnableStageAppender(String name, Session session) {
		var stk = outerStackTraceElement(); // !important keep out it of consumer
		return (s,e,o,t)->{
			var stg = new LocalRequest();
			stg.setStart(s);
			stg.setEnd(e);
			stg.setException(mainCauseException(t));
			stg.setName(name);
			stk.ifPresent(st-> {
				if(isNull(name)) {
					stg.setName(st.getMethodName());
				}
				stg.setLocation(st.getClassName());
			});
			appendSessionStage(session, stg);
		};
	}
	
	static boolean appendSessionStage(SessionStage stg) {
		return appendSessionStage(localTrace.get(), stg);
	}

	private static boolean appendSessionStage(Session session, SessionStage stg) {
		if(nonNull(session)) {
			session.append(stg);
			return true;
		}
		warnNoActiveSession(stg); //log untracked stage
		return false;
	}
}
