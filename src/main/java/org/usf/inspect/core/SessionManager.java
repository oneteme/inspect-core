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

	private static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();
	private static MainSession startupSession; //avoid setting startup session on all thread local

	public static <S extends Session> S requireCurrentSession(Class<S> clazz) {
		var ses = requireCurrentSession();
		if(clazz.isInstance(ses)) { //nullable
			return clazz.cast(ses);
		}
		if(nonNull(ses)) {
			reportIllegalSessionState("unexpected session type", ses);
		}
		return null;
	}

	public static Session requireCurrentSession() {
		var ses = currentSession();
		if(isNull(ses)) {
			reportIllegalSessionState("no current session found", null);
		}
		else if(ses.wasCompleted()) {
			reportIllegalSessionState("current session was already completed", ses);
			ses = null;
		}
		return ses;
	}

	public static Session currentSession() {
		var ses = localTrace.get();
		return nonNull(ses) ? ses : startupSession; // priority
	}
	
	static void setCurrentSession(Session session) {
		var prv = localTrace.get();
		if(prv != session) {
			if(isNull(prv) || prv.wasCompleted()) {
				localTrace.set(session);
			}
			else {
				reportSessionConflict("setCurrentSession", prv.getId(), session.getId());
			}
		}
	}
	
	static void releaseSession(Session session) {
		var prv = localTrace.get();
		if(prv == session) {
			localTrace.remove();
		}
		else if(nonNull(prv)) {
			reportSessionConflict("releaseSession", prv.getId(), session.getId());
		}
	}
	
	static void setStartupSession(MainSession session) {
		if(isNull(startupSession)) {
			startupSession = session;
		}
		else {
			reportSessionConflict("setStartupSession", startupSession.getId(), session.getId());
		}
	}
	
	static void releaseStartupSession(MainSession session) {
		if(startupSession == session) {
			startupSession = null;
		}
		else if(nonNull(startupSession)) {
			reportSessionConflict("releaseStartupSession", startupSession.getId(), session.getId());
		}
	}

	public static <E extends Throwable> void trackRunnable(LocalRequestType type, String name, SafeRunnable<E> fn) throws E {
		trackCallble(type, name, fn);
	}

	public static <T, E extends Throwable> T trackCallble(LocalRequestType type, String name, SafeCallable<T,E> fn) throws E {
		var ste = outerStackTraceElement();
		return call(fn, asynclocalRequestListener(type, 
				()-> ste.map(e-> formatLocation(e.getClassName(), e.getMethodName())).orElse("?"), 
				()-> nonNull(name) ? name : ste.map(StackTraceElement::getMethodName).orElse("?")));
	}

	static <T> ExecutionHandler<T> asynclocalRequestListener(LocalRequestType type, Supplier<String> locationSupp, Supplier<String> nameSupp) {
		var now = now();
		var req = createLocalRequest();
		call(()->{
			req.setStart(now);
			req.setThreadName(threadName());
			req.setType(type.name());
			req.setName(nameSupp.get());
			req.setLocation(locationSupp.get());
			return req;
		});
		return (s,e,o,t)->{
			req.runSynchronized(()-> {
				if(nonNull(t)) {
					req.setException(fromException(t));
				}
				req.setEnd(e);
			});
			return req;
		};
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
		reporter().action(action).message(format("previous=%s, next=%s", prev, next)).emit();
	}

	static void reportIllegalSessionState(String msg, Session session) {
		reporter().action("requireCurrentSession").message(msg).trace(session).emit();
	}
}
