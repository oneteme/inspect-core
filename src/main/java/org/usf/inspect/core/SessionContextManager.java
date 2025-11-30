package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.ErrorReporter.stackReporter;
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

	private static final ThreadLocal<AbstractSessionCallback> localTrace = new InheritableThreadLocal<>();
	private static AbstractSessionCallback startupContext; //avoid setting startup session on all thread local
	
    public static Runnable aroundRunnable(Runnable cmd) {
    	var ses = requireActiveContext();
		if(nonNull(ses)) {
			ses.threadCountUp();
			return ()-> aroundRunnable(cmd::run, ses, ses::threadCountDown);
		}
		return cmd;
    }
    
    public static <T> Callable<T> aroundCallable(Callable<T> cmd) {
    	var ses = requireActiveContext();
		if(nonNull(ses)) {
			ses.threadCountUp();
			return ()-> aroundCallable(cmd::call, ses, ses::threadCountDown);
		}
		return cmd;
    }
    
    public static <T> Supplier<T> aroundSupplier(Supplier<T> cmd) {
    	var ses = requireActiveContext();
    	if(nonNull(ses)) {
			ses.threadCountUp();
			return ()-> aroundCallable(cmd::get, ses, ses::threadCountDown);
		}
		return cmd;
    }

    static <E extends Exception> void aroundRunnable(SafeRunnable<E> task, AbstractSessionCallback ctx, Runnable callback) throws E {
    	aroundCallable(task, ctx, callback);
    }
    
    static <T, E extends Exception> T aroundCallable(SafeCallable<T, E> call, AbstractSessionCallback ctx, Runnable callback) throws E {
		var prv = activeContext();
		if(prv != ctx) {
			setActiveContext(ctx);
		}
		try {
			return call.call();
		}
		finally {
			if(prv != ctx) {
				clearContext(ctx);
				if(nonNull(prv)) {
					setActiveContext(prv);
				}
			}
			if(nonNull(callback)) {
				callback.run();
			}
		}	
	}
	
	public static AbstractSessionCallback requireActiveContext() {
		var ses = activeContext();
		if(isNull(ses)) {
			reportNoActiveContext("requireActiveContext");
		}
		else if(ses.wasCompleted()){
			reportIllegalContextState("requireActiveContext", "current context was already completed");
			ses = null;
		}
		return ses;
	}

	public static AbstractSessionCallback activeContext() {
		var trc = localTrace.get();
		return nonNull(trc) ? trc : startupContext; // priority
	}

	public static void setActiveContext(AbstractSessionCallback session) {
		if(session.isStartup()) {
			if(startupContext != session) {
				if(isNull(startupContext)) {
					startupContext = session;
				}
				else {
					reportContextConflict("setActiveContext", startupContext.getId(), session.getId());
				}
			}
		}
		else {
			var prv = localTrace.get();
			if(prv != session) {
				localTrace.set(session);
			}
		}
	}
	
	public static void clearContext(AbstractSessionCallback ctx) {
		if(ctx.isStartup()) {
			if(startupContext == ctx) {
				if(ctx.wasCompleted()) { //reactor
					startupContext = null;
				}
			}
			else if(nonNull(startupContext)) {
				reportContextConflict("clearContext", startupContext.getId(), ctx.getId());
			}
			else {
				reportNoActiveContext("clearContext");
			}
		}
		else {
			var prv = localTrace.get();
			if(prv == ctx) {
				localTrace.remove();
			}
			else if(nonNull(prv)) {
				reportContextConflict("clearContext", prv.getId(), ctx.getId());
			}
			else {
				reportNoActiveContext("clearContext");
			}
		}
	}

	public static HttpSession2 createHttpSession(Instant start, String uuid) {
		var ses = new HttpSession2(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
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
		return createHttpRequest(nextId(), start);
	}
	
	public static HttpRequest2 createHttpRequest(String id, Instant start) {
		return new HttpRequest2(id, requireSessionIdFor(REST), start, threadName());
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
		var ses = requireActiveContext();
		if(nonNull(ses)) {
			if(ses.updateMask(mask) && ses.isAsync()) {
				ses.emit();
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
		var ctx = requireActiveContext();
		if(nonNull(ctx)) {
			log.setSessionId(ctx.getId());
		}
		log.emit();
	}

	public static String nextId() {
		return randomUUID().toString();
	}

	public static void reportContextIsNull(String action) {
		stackReporter().action(action).message("session is null").thread().emit();
	}
	
	static void reportNoActiveContext(String action) {
		stackReporter().action(action).message("no active context").thread().emit();
	}
	
	static void reportContextConflict(String action, String prev, String next) {
		stackReporter().action(action).message(format("previous=%s, next=%s", prev, next)).thread().emit();
	}

	static void reportIllegalContextState(String action, String msg) {
		stackReporter().action(action).message(msg).thread().emit();
	}
}
