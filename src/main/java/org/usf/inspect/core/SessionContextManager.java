package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.Helper.threadName;
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

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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
public final class SessionContextManager {

	private static final ThreadLocal<SessionContext> localTrace = new InheritableThreadLocal<>();
	private static SessionContext startupContext; //avoid setting startup session on all thread local
	
    public static Runnable aroundRunnable(Runnable cmd) {
    	var ses = requireCurrentSession();
		if(nonNull(ses)) {
			ses.callback.threadCountUp();
			return ()-> aroundRunnable(cmd::run, ses, ses.callback::threadCountDown);
		}
		return cmd;
    }
    
    public static <T> Callable<T> aroundCallable(Callable<T> cmd) {
    	var ses = requireCurrentSession();
		if(nonNull(ses)) {
			ses.callback.threadCountUp();
			return ()-> aroundCallable(cmd::call, ses, ses.callback::threadCountDown);
		}
		return cmd;
    }
    
    public static <T> Supplier<T> aroundSupplier(Supplier<T> cmd) {
    	var ses = requireCurrentSession();
    	if(nonNull(ses)) {
			ses.callback.threadCountUp();
			return ()-> aroundCallable(cmd::get, ses, ses.callback::threadCountDown);
		}
		return cmd;
    }

    static <E extends Exception> void aroundRunnable(SafeRunnable<E> task, SessionContext session, Runnable callback) throws E {
    	aroundCallable(task, session, callback);
    }
    
    static <T, E extends Exception> T aroundCallable(SafeCallable<T, E> call, SessionContext session, Runnable callback) throws E {
		var prv = currentSession();
		if(prv != session) {
			setCurrentSession(session);
		}
		try {
			return call.call();
		}
		finally {
			if(prv != session) {
				releaseSession(session);
				if(nonNull(prv)) {
					setCurrentSession(prv);
				}
			}
			if(nonNull(callback)) {
				callback.run();
			}
		}	
	}
	
	public static SessionContext requireCurrentSession() {
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

	public static SessionContext currentSession() {
		var trc = localTrace.get();
		return nonNull(trc) ? trc : startupContext; // priority
	}

	public static void setCurrentSession(SessionContext session) {
		var prv = localTrace.get();
		if(prv != session) {
			localTrace.set(session);
		}
	}
	
	public static void releaseSession(SessionContext session) {
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
	
	static void setStartupSession(SessionContext session) {
		if(startupContext != session) {
			if(isNull(startupContext)) {
				startupContext = session;
			}
			else {
				reportSessionConflict("setStartupSession", startupContext.getId(), session.getId());
			}
		}
	}
	
	static void releaseStartupSession(SessionContext ctx) {
		if(startupContext == ctx) {
			if(ctx.wasCompleted()) { //reactor
				startupContext = null;
			}
		}
		else if(nonNull(startupContext)) {
			reportSessionConflict("releaseStartupSession", startupContext.getId(), ctx.getId());
		}
		else {
			reportNoActiveSession("releaseStartupSession", ctx);
		}
	}

	public static HttpSession2 createHttpSession(Instant start, String uuid) {
		var ses = new HttpSession2(uuid, start, threadName());
		ses.setLinked(nonNull(uuid));
		return ses;
	}

	public static MainSession2 createStartupSession(Instant start) {
		return createMainSession(STARTUP, start);
	}

	public static MainSession2 createBatchSession(Instant start) {
		return createMainSession(BATCH, start);
	}
	
	public static MainSession2 createTestSession(Instant start) {
		return createMainSession(TEST, start);
	}
	
	static MainSession2 createMainSession(MainSessionType type, Instant start) {
		return new MainSession2(nextId(), start, threadName(), type.name());
	}

	public static LocalRequest2 createLocalRequest(Instant start) {
		return new LocalRequest2(nextId(), requireSessionIdFor(LOCAL), start, threadName());
	}
	
	public static DatabaseRequest2 createDatabaseRequest(Instant start) {
		return new DatabaseRequest2(nextId(), requireSessionIdFor(JDBC), start, threadName());
	}
	
	public static HttpRequest2 createHttpRequest(Instant start) {
		return new HttpRequest2(nextId(), requireSessionIdFor(REST), start, threadName());
	}

	public static FtpRequest2 createFtpRequest(Instant start) {
		return new FtpRequest2(nextId(), requireSessionIdFor(FTP), start, threadName());
	}
	
	public static MailRequest2 createMailRequest(Instant start) {
		return new MailRequest2(nextId(), requireSessionIdFor(SMTP), start, threadName());
	}

	public static DirectoryRequest2 createNamingRequest(Instant start) {
		return new DirectoryRequest2(nextId(), requireSessionIdFor(LDAP), start, threadName());
	}
	
	private static String requireSessionIdFor(RequestMask mask) {
		var ctx = requireCurrentSession();
		if(nonNull(ctx)) {
			var ses = ctx.callback;
			if(ses.updateMask(mask) && ses.isAsync()) {
				new MaskChangeTrace(ses.getId(), ses.getRequestMask().get()).emit();
			}
			return ses.getId();
		}
		return null;
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

	public static void reportSessionIsNull(String action) {
		reporter().action(action).message("session is null").thread().emit();
	}
	
	static void reportNoActiveSession(String action, AbstractSessionCallback session) {
		reporter().action(action).message("no active session").trace(session).thread().emit();
	}
	
	static void reportSessionConflict(String action, String prev, String next) {
		reporter().action(action).message(format("previous=%s, next=%s", prev, next)).thread().emit();
	}

	static void reportIllegalSessionState(String action, String msg, AbstractSessionCallback session) {
		reporter().action(action).message(msg).trace(session).thread().emit();
	}
}
