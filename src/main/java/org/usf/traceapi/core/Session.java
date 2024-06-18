package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.outerStackTraceElement;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;
import static org.usf.traceapi.core.StageTracker.call;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.traceapi.core.SafeCallable.SafeRunnable;
import org.usf.traceapi.core.StageTracker.StageConsumer;

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
	
	Collection<RestRequest> getRequests();	  // rename to getApiRequests
	
	Collection<DatabaseRequest> getQueries(); //rename to getDatabaseRequests

	Collection<RunnableStage> getStages();
	
	Collection<FtpRequest> getFtpRequests();

	Collection<MailRequest> getMailRequests();
	
	AtomicInteger getLock();
	
	default void append(SessionStage stage) {
		if(stage instanceof RestRequest rest) {
			getRequests().add(rest);
		}
		else if(stage instanceof DatabaseRequest db) {
			getQueries().add(db);
		}
		else if(stage instanceof FtpRequest ftp) {
			getFtpRequests().add(ftp);
		}
		else if(stage instanceof MailRequest mail) {
			getMailRequests().add(mail);
		}
		else if(stage instanceof RunnableStage run) {
			getStages().add(run);
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
			var stg = new RunnableStage();
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
