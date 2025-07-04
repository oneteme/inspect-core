package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.Helper.warnStackTrace;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.LogEntry.Level.ERROR;
import static org.usf.inspect.core.LogEntry.Level.INFO;
import static org.usf.inspect.core.LogEntry.Level.WARN;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.inspect.core.TraceBroadcast.emit;

import java.util.function.Function;
import java.util.function.Supplier;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.LogEntry.Level;
import org.usf.inspect.core.SafeCallable.SafeRunnable;

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
		else if(ses.wasCompleted()) {
			warnStackTrace("current session already completed: " + ses);
			ses = null;
		}
		return ses;
	}
	
	public static Session currentSession() {
		var ses = localTrace.get(); // priority
		return nonNull(ses) ? ses : startupSession;
	}
	
	@Deprecated
	public static void updateCurrentSession(Session s) {
		if(localTrace.get() != s) { // null || local previous session
			localTrace.set(s);
		}
	}
	
	public static SessionContextUpdater sessionContextUpdater() {
		var ses = requireCurrentSession();
		return nonNull(ses) ? ()-> updateCurrentSession(ses) : null;
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
		return ses;
	}
	
	public static RestSession startRestSession() {
		var ses = new RestSession();
		ses.setId(nextId());
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
		trackCallble(name, fn);
	}
	
	public static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
		var loc = stackLocation();
		return call(fn, localRequestListener(o->{
			var req = startRequest(LocalRequest::new);
			req.setName(name);
			req.setLocation(loc); //outside task
			req.setType(EXEC.name());
			return req;
		}));
	}
	
	public static <T> ExecutionMonitorListener<T> localRequestListener(Function<T, LocalRequest> fn) {
		return (s,e,o,t)-> {
			var req = fn.apply(o);
			req.setThreadName(threadName()); 
			req.setStart(s);
			req.setEnd(e);
			if(nonNull(t)) {
				req.setException(mainCauseException(t));
			}
			emit(req);
		};
	}
	
	public static <T> ExecutionMonitorListener<T> asynclocalRequestListener(LocalRequestType type, Supplier<String> locationSupp, Supplier<String> nameSupp) {
		var req = startRequest(LocalRequest::new);
    	try {
        	req.setStart(now());
        	req.setThreadName(threadName());
			req.setType(type.name());
        	req.setName(nameSupp.get());
        	req.setLocation(locationSupp.get());
    	}
    	finally {
			emit(req);
		}
		return (s,e,o,t)-> req.lazy(()-> {
    		if(nonNull(t)) {
				req.setException(mainCauseException(t));
			}
			req.setEnd(e);
			emit(req);
		});
	}
	
	private static String stackLocation() {
		return outerStackTraceElement()
				.map(StackTraceElement::getClassName)
				.orElse(null);
	}
	
	public static <T extends AbstractRequest> T startRequest(Supplier<T> supp) {
		var req = supp.get();
		var ses = requireCurrentSession();
		req.setId(nextId());
		if(nonNull(ses)) {
			req.setSessionId(ses.getId());
		}
		return req;
	}
	
	public void info(String msg) {
		trace(INFO, msg);
	}

	public void warn(String msg) {
		trace(WARN, msg);
	}
	
	public void error(String msg) {
		trace(ERROR, msg);
	}
	
	public void trace(Level lvl, String msg) {
		var log = new LogEntry(now(), lvl, msg);
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			log.setSessionId(ses.getId());
		}
		emit(log);
	}
	
	@FunctionalInterface
	public interface SessionContextUpdater {

		void updateContext();
	}
}
