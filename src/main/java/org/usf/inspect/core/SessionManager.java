package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.Helper.outerStackTraceElement;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.LocalRequestType.EXEC;
import static org.usf.inspect.core.LogEntry.logEntry;
import static org.usf.inspect.core.LogEntry.Level.ERROR;
import static org.usf.inspect.core.LogEntry.Level.INFO;
import static org.usf.inspect.core.LogEntry.Level.WARN;
import static org.usf.inspect.core.MainSessionType.BATCH;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.RequestMask.FTP;
import static org.usf.inspect.core.RequestMask.JDBC;
import static org.usf.inspect.core.RequestMask.LDAP;
import static org.usf.inspect.core.RequestMask.LOCAL;
import static org.usf.inspect.core.RequestMask.REST;
import static org.usf.inspect.core.RequestMask.SMTP;

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
		if(nonNull(ses)) {
			context().reportError("unexpected session type: " + ses.getId());
		}
		return null;
	}

	public static Session requireCurrentSession() {
		var ses = currentSession();
		if(isNull(ses)) {
			context().reportError("no current session found");
		}
		else if(ses.wasCompleted()) {
			context().reportError("current session was already completed: " + ses.getId());
			ses = null;
		}
		return ses;
	}

	public static Session currentSession() {
		var ses = localTrace.get();
		return nonNull(ses) ? ses : startupSession; // priority
	}

	public static void emitSessionStart(Session session) {
		context().emitTrace(session);
		setCurrentSession(session);
	}

	static void setCurrentSession(Session ses) {
		var prv = localTrace.get();
		if(prv != ses) {
			if(isNull(prv) || prv.wasCompleted()) {
				localTrace.set(ses);
			}
			else {
				reportSessionConflict(prv.getId(), ses.getId());
			}
		}// else do nothing, already set
	}

	public static void emitSessionEnd(Session session) {
		context().emitTrace(session);
		releaseSession(session);
	}

	static void releaseSession(Session ses) {
		var prv = localTrace.get();
		if(prv == ses) {
			localTrace.remove();
		}
		else {
			reportSessionConflict(prv.getId(), ses.getId());
		}
	}

	public static void emitStartupSession(MainSession session) {
		context().emitTrace(session);
		if(isNull(startupSession)) {
			startupSession = session;
		}
		else {
			reportSessionConflict(startupSession.getId(), session.getId());
		}
	}

	public static void emitStartupSesionEnd(MainSession session) {
		context().emitTrace(startupSession);
		if(startupSession == session) {
			startupSession = null;
		}
		else {
			reportSessionConflict(startupSession.getId(), session.getId());
		}
	}

	public static <E extends Throwable> void trackRunnable(String name, SafeRunnable<E> fn) throws E {
		trackCallble(name, fn);
	}

	public static <T, E extends Throwable> T trackCallble(String name, SafeCallable<T,E> fn) throws E {
		var loc = callerLocation();
		return call(fn, asynclocalRequestListener(EXEC, ()-> loc, ()-> name));
	}

	static <T> ExecutionMonitorListener<T> asynclocalRequestListener(LocalRequestType type, Supplier<String> locationSupp, Supplier<String> nameSupp) {
		var now = now();
		var req = createLocalRequest();
		try {
			req.setStart(now);
			req.setThreadName(threadName());
			req.setType(type.name());
			req.setName(nameSupp.get());
			req.setLocation(locationSupp.get());
		}
		catch (Exception e) {
			context().reportEventHandleError(req.getId(), e);
		}
		context().emitTrace(req);
		return (s,e,o,t)->{
			req.runSynchronized(()-> {
				if(nonNull(t)) {
					req.setException(fromException(t));
				}
				req.setEnd(e);
			});
			context().emitTrace(req);
		};
	}

	private static String callerLocation() {
		return outerStackTraceElement()
				.map(StackTraceElement::getClassName)
				.orElse(null);
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
		var log = logEntry(lvl, msg);
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			log.setSessionId(ses.getId());
		}
		context().emitTrace(log);
	}

	public static String nextId() {
		return randomUUID().toString();
	}

	static void reportSessionConflict(String prev, String next) {
		context().reportError(format("session conflict detected : previous=%s, next=%s", prev, next));
	}
}
