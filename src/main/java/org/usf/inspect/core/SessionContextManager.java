package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.InspectContext.context;
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
		System.err.println("setActiveContext " + session.getId() + " ~ " + Helper.threadName());
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
		System.err.println("clearContext " + ctx.getId() + " ~ " + Helper.threadName());
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

	static MainSession2 createStartupSession(Instant start, String uuid) {
		return createMainSession(STARTUP, start, requireNonNullElseGet(uuid, SessionContextManager::nextId));
	}

	public static MainSession2 createBatchSession(Instant start) {
		return createMainSession(BATCH, start, nextId());
	}
	
	public static MainSession2 createTestSession(Instant start) {
		return createMainSession(TEST, start, nextId());
	}
	
	static MainSession2 createMainSession(MainSessionType type, Instant start, String uuid) {
		return new MainSession2(uuid, start, threadName(), type.name());
	}

	public static LocalRequest2 createLocalRequest(Instant start) {
		return new LocalRequest2(nextId(), requireSessionIdFor(LOCAL), start, threadName());
	}
	
	public static DatabaseRequest2 createDatabaseRequest(Instant start) {
		return new DatabaseRequest2(nextId(), requireSessionIdFor(JDBC), start, threadName());
	}
	
	public static HttpRequest2 createHttpRequest(Instant start, String rid) {
		return new HttpRequest2(rid, requireSessionIdFor(REST), start, threadName());
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
	
	static String requireSessionIdFor(RequestMask mask) {
		var ses = requireActiveContext();
		if(nonNull(ses)) {
			if(ses.updateMask(mask)) {
				context().emitTrace(new SessionMaskUpdate(ses.getId(), ses instanceof MainSessionCallback, ses.getRequestMask().get()));
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
		var log = logEntry(lvl, msg); // no stack
		var ctx = requireActiveContext();
		if(nonNull(ctx)) {
			log.setSessionId(ctx.getId());
		}
		context().emitTrace(ctx);
	}

	public static String nextId() {
		return randomUUID().toString();
	}

	static void reportNoActiveContext(String action) {
		context().reportMessage(true, action, "no active context");
	}
	
	static void reportContextConflict(String action, String prev, String next) {
		context().reportMessage(true, action, format("previous=%s, next=%s", prev, next));
	}

	static void reportIllegalContextState(String action, String msg) {
		context().reportMessage(true, action, msg);
	}
}
