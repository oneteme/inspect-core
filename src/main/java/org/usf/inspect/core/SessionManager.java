package org.usf.inspect.core;

import static java.util.Collections.synchronizedList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.warnNoActiveSession;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
	private static MainSession startupSession;
	private static Supplier<Session> lookup = SessionManager::currentSession;

	public static Session currentSession() {
		var ses = localTrace.get();
		if(nonNull(ses) && ses.completed()) {
//			log.warn("current session was completed {}", ses);
		}
		return ses;
	}
	
	public static void updateCurrentSession(Session s) {
		if(localTrace.get() != s) { // null || local previous session
			localTrace.set(s);
		}
	}
	
	public static MainSession startBatchSession() {
		var ses = mainSession();
		localTrace.set(ses);
		return ses;
	}
	
	public static MainSession startupSession() {
		if(isNull(startupSession)) {
			startupSession = mainSession();
		}
		else {
			log.warn("startup session already exists {}", startupSession);
		}
		return startupSession;
	}
	
	private static MainSession mainSession() {
		var ses = new MainSession();
		ses.setId(nextId());
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
			warnNoActiveSession("");
		}
		return ses;
	}
	
	static <T> List<T> synchronizedArrayList() {
		return synchronizedList(new ArrayList<>());
	}

	public static boolean appendSessionStage(SessionStage stg) {
		var session = currentSession();
		if(nonNull(session)) {
			session.append(stg);
			return true;
		}
		warnNoActiveSession(stg); //log untracked stage
		return false;
	}
	
	static <E extends Throwable> void trackRunnable(String name, SafeRunnable<E> fn) throws E {
		exec(fn, localRequestAppender(name));
	}
	
	static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
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
