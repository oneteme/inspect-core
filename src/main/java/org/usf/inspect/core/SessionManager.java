package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.synchronizedArrayList;
import static org.usf.inspect.core.Helper.warnStackTrace;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;

import org.usf.inspect.core.SafeCallable.SafeRunnable;
import org.usf.inspect.core.StageTracker.StageConsumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionManager {

	private static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();
	private static MainSession startupSession; //avoid setting startup session on all thread local

	public static <S extends Session> S requireCurrentSession(Class<S> clazz) {
		var ses = requireCurrentSession();
		if(clazz.isInstance(ses)) { //nullable
			return clazz.cast(ses);
		}
		log.warn("unexpected session type: expected={}, but was={}", clazz.getSimpleName(), ses);
		return null;
	}
	
	public static Session requireCurrentSession() {
		var ses = currentSession();
		if(isNull(ses)) {
			warnStackTrace("no active session");
		}
		else if(ses.completed()) {
			warnStackTrace("current session already completed: " + ses);
		}
		return ses;
	}
	
	public static Session currentSession() {
		var ses = localTrace.get(); // priority
		return nonNull(ses) ? ses : startupSession;
	}
	
	public static void updateCurrentSession(Session s) {
		if(localTrace.get() != s) { // null || local previous session
			localTrace.set(s);
		}
	}
	
	public static MainSession startBatchSession() {
		var ses = mainSession(BATCH);
		localTrace.set(ses);
		return ses;
	}
	
	public static MainSession startupSession() {
		if(isNull(startupSession)) {
			startupSession = mainSession(STARTUP);
		}
		else {
			log.warn("startup session already exists {}", startupSession);
		}
		return startupSession;
	}
	
	private static MainSession mainSession(MainSessionType type) {
		var ses = new MainSession();
		ses.setId(nextId());
		ses.setType(type.name());
		ses.setRestRequests(synchronizedArrayList());
		ses.setDatabaseRequests(synchronizedArrayList());
		ses.setFtpRequests(synchronizedArrayList());
		ses.setMailRequests(synchronizedArrayList());
		ses.setLdapRequests(synchronizedArrayList());
		ses.setLocalRequests(synchronizedArrayList());
		return ses;
	}
	
	public static RestSession startRestSession() {
		var ses = new RestSession();
		ses.setId(nextId());
		ses.setRestRequests(synchronizedArrayList());
		ses.setDatabaseRequests(synchronizedArrayList());
		ses.setFtpRequests(synchronizedArrayList());
		ses.setMailRequests(synchronizedArrayList());
		ses.setLdapRequests(synchronizedArrayList());
		ses.setLocalRequests(synchronizedArrayList());
		localTrace.set(ses);
		return ses;
	}	
	
	public static Session endSession() {
		var ses = localTrace.get();
		if(nonNull(ses)) {
			localTrace.remove();
		}
		else {
			warnStackTrace("no active session");
		}	
		return ses;
	}
	
	public static MainSession endStatupSession() {
		var ses = startupSession;
		if(nonNull(startupSession)) {
			startupSession = null;
		}
		else {
			warnStackTrace("no startup session");
		}
		return ses;
	}

	public static boolean appendSessionStage(SessionStage stg) {
		var ses = requireCurrentSession();
		return nonNull(ses) && ses.append(stg);
	}
	
	public static <E extends Throwable> void trackRunnable(String name, SafeRunnable<E> fn) throws E {
		exec(fn, localRequestAppender(name));
	}
	
	public static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
		return call(fn, localRequestAppender(name));
	}

	static StageConsumer<Object> localRequestAppender(String name) {
		return (s,e,o,t)->{
			var stg = new LocalRequest();
			stg.setStart(s);
			stg.setEnd(e);
			stg.setException(mainCauseException(t));
			stg.setName(name);
			outerStackTraceElement().ifPresent(st-> {
				if(isNull(name)) {
					stg.setName(st.getMethodName());
				}
				stg.setLocation(st.getClassName());
			});
			appendSessionStage(stg);
		};
	}
}
