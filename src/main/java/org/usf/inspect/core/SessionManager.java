package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LocalRequest.formatLocation;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;
import static org.usf.inspect.core.LogEntry.Level.INFO;
import static org.usf.inspect.core.LogEntry.Level.WARN;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.MainSessionType.TEST;
import static org.usf.inspect.core.RequestMask.FTP;
import static org.usf.inspect.core.RequestMask.JDBC;
import static org.usf.inspect.core.RequestMask.LDAP;
import static org.usf.inspect.core.RequestMask.LOCAL;
import static org.usf.inspect.core.RequestMask.REST;
import static org.usf.inspect.core.RequestMask.SMTP;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;
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

	private static final ThreadLocal<AbstractSession> localTrace = new InheritableThreadLocal<>();
	private static MainSession startupSession; //avoid setting startup session on all thread local
	
    public static Runnable aroundRunnable(Runnable cmd) {
    	var ses = requireCurrentSession();
		return nonNull(ses) ? ses.createContext().aroundRunnable(cmd) : cmd;
    }

    public static <T> Callable<T> aroundCallable(Callable<T> cmd) {
    	var ses = requireCurrentSession();
		return nonNull(ses) ? ses.createContext().aroundCallable(cmd) : cmd;
    }
    
    public static <T> Supplier<T> aroundSupplier(Supplier<T> cmd) {
    	var ses = requireCurrentSession();
		return nonNull(ses) ? ses.createContext().aroundSupplier(cmd) : cmd;
    }
	
	public static <S extends AbstractSession> S requireCurrentSession(Class<S> clazz) {
		var ses = requireCurrentSession();
		if(clazz.isInstance(ses)) { //nullable
			return clazz.cast(ses);
		}
		if(nonNull(ses)) {
			reportIllegalSessionState("requireCurrentSession", "unexpected session type", ses);
		}
		return null;
	}

	public static AbstractSession requireCurrentSession() {
		var ses = currentSession();
		if(isNull(ses)) {
			reportNoActiveSession("requireCurrentSession", null);
		}
		else if(ses.wasCompleted()){
			reportIllegalSessionState("requireCurrentSession", "current session was already completed", ses);
			ses = null;
		}
		return ses;
	}

	public static AbstractSession currentSession() {
		var trc = localTrace.get();
		return nonNull(trc) ? trc : startupSession; // priority
	}
	
	static void setCurrentSession(AbstractSession session) {
		var prv = localTrace.get();
		if(prv != session) {
			localTrace.set(session);
		}
	}
	
	static void releaseSession(AbstractSession session) {
		var prv = localTrace.get();
		if(prv == session) {
			localTrace.remove();
		}
		else if(nonNull(prv)) {
			reportSessionConflict("releaseSession", prv.getId(), session.getId());
		}
		else {
			reportNoActiveSession("releaseSession", session);
		}
	}
	
	static void setStartupSession(MainSession session) {
		if(startupSession != session) {
			if(isNull(startupSession)) {
				startupSession = session;
			}
			else {
				reportSessionConflict("setStartupSession", startupSession.getId(), session.getId());
			}
		}
	}
	
	static void releaseStartupSession(MainSession session) {
		if(startupSession == session) {
			if(nonNull(session.getEnd())) { //reactor
				startupSession = null;
			}
		}
		else if(nonNull(startupSession)) {
			reportSessionConflict("releaseStartupSession", startupSession.getId(), session.getId());
		}
		else {
			reportNoActiveSession("releaseStartupSession", session);
		}
	}

	public static <E extends Throwable> void trackRunnable(LocalRequestType type, String name, SafeRunnable<E> fn) throws E {
		trackCallble(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallble(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		var ste = outerStackTraceElement();
		return call(fn, localRequestHandler(type, 
				()-> nonNull(name) ? name : ste.map(StackTraceElement::getMethodName).orElse(null),
				()-> ste.map(e-> formatLocation(e.getClassName(), e.getMethodName())).orElse(null), 
				()-> null));
	}

	public static <T> ExecutionHandler<T> localRequestHandler(LocalRequestType type, Supplier<String> nameSupp, Supplier<String> locationSupp, Supplier<String> userSupp) {
		var req = createLocalRequest();
		call(()->{
			req.setStart(now());
			req.setThreadName(threadName());
			req.setType(type.name());
			req.setName(nameSupp.get());
			req.setLocation(locationSupp.get());
			req.setUser(userSupp.get());
			req.emit();
		});
		return (s,e,o,t)-> req.runSynchronized(()-> {
			req.setStart(s);
			if(nonNull(t)) {
				req.setException(fromException(t));
			}
			req.setEnd(e);
		});
	}
	
	public static RestSession createRestSession() {
		return createRestSession(nextId());
	}

	public static RestSession createRestSession(String uuid) {
		var session = new RestSession();
		session.setId(uuid);
		return session;
	}

	public static MainSession createStartupSession() {
		return createMainSession(STARTUP);
	}

	public static MainSession createBatchSession() {
		return createMainSession(BATCH);
	}

	public static MainSession createTestSession() {
		return createMainSession(TEST);
	}
	
	static MainSession createMainSession(MainSessionType type) {
		var ses = new MainSession();
		ses.setId(nextId());
		ses.setType(type.name());
		return ses;
	}

	public static RestRequest createHttpRequest() {
		return traceableRequest(REST, new RestRequest());
	}

	public static DatabaseRequest createDatabaseRequest() {
		return traceableRequest(JDBC, new DatabaseRequest());
	}

	public static FtpRequest createFtpRequest() {
		return traceableRequest(FTP, new FtpRequest());
	}

	public static MailRequest createMailRequest() {
		return traceableRequest(SMTP, new MailRequest());
	}

	public static DirectoryRequest createNamingRequest() {
		return traceableRequest(LDAP, new DirectoryRequest());
	}

	public static LocalRequest createLocalRequest() {
		return traceableRequest(LOCAL, new LocalRequest());
	}

	static <T extends AbstractRequest> T traceableRequest(RequestMask mask, T req) {
		req.setId(nextId());
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			req.setSessionId(ses.getId());
			ses.updateRequestsMask(mask);
		}
		return req;
	}

	public static void emitInfo(String msg) {
		emitLog(INFO, msg);
	}

	public static void emitWarn(String msg) {
		emitLog(WARN, msg);
	}

	public static void emitError(String msg) {
		emitLog(ERROR, msg);
	}

	private static void emitLog(Level lvl, String msg) {
		var log = logEntry(lvl, msg, 0); // no stack
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			log.setSessionId(ses.getId());
		}
		log.emit();
	}

	public static String nextId() {
		return randomUUID().toString();
	}

	static void reportSessionConflict(String action, String prev, String next) {
		reporter().action(action).message(format("previous=%s, next=%s", prev, next)).thread().emit();
	}

	static void reportNoActiveSession(String action, AbstractSession session) {
		reporter().action(action).message("no active session").trace(session).thread().emit();
	}
	
	static void reportIllegalSessionState(String action, String msg, AbstractSession session) {
		reporter().action(action).message(msg).trace(session).thread().emit();
	}
}
