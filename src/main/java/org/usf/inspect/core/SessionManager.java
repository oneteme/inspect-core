package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.synchronizedArrayList;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.Helper.warnStackTrace;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;

import org.usf.inspect.core.SafeCallable.SafeRunnable;
import org.usf.inspect.core.StageTracker.SafeConsumer;
import org.usf.inspect.core.StageTracker.StageCreator;

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
			ses = null;
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

	public static <E extends Throwable> void trackRunnable(String name, SafeRunnable<E> fn) throws E {
		exec(fn, localRequestCreator(name, stackLocation(), EXEC), requestAppender());
	}
	
	public static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
		return call(fn, localRequestCreator(name, stackLocation(), EXEC), requestAppender());
	}

	public static <T> StageCreator<T, LocalRequest> localRequestCreator(String name, String location, LocalRequestType type) {
		return (s,e,o,t)->{
			var req = new LocalRequest();
			req.setName(name);
			req.setLocation(location);
			req.setStart(s);
			req.setEnd(e);
			req.setThreadName(threadName());
			req.setType(isNull(type) ? null : type.name());
			if(nonNull(t)) {
				req.setException(mainCauseException(t));
			}
			return req;
		};
	}
	
	private static String stackLocation() {
		return outerStackTraceElement()
				.map(st-> formatLocation(st.getClassName(), st.getMethodName()))
				.orElse(null);
	}
	
	@Deprecated
	public static SafeConsumer<SessionStage> requestAppender() {
		var ses = requireCurrentSession();
		return isNull(ses) ? req->{} : ses::append;
	}
	

	public static void appendStage(SessionStage stage) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			ses.append(stage);
		}
	}
}
