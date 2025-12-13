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
import org.usf.inspect.core.SafeCallable.SafeConsumer;
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

	public static HttpSessionCallback createHttpSession(Instant start, String uuid, SafeConsumer<HttpSession2> consumer) {
		var ses = new HttpSession2(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
		ses.setLinked(nonNull(uuid));
		return completeAndEmit(ses, consumer).createCallback();
	}

	public static MainSessionCallback createStartupSession(Instant start, String uuid, SafeConsumer<MainSession2> consumer) {
		return createMainSession(STARTUP, start, requireNonNullElseGet(uuid, SessionContextManager::nextId), consumer);
	}

	public static MainSessionCallback createBatchSession(Instant start, SafeConsumer<MainSession2> consumer) {
		return createMainSession(BATCH, start, nextId(), consumer);
	}
	
	public static MainSessionCallback createTestSession(Instant start, SafeConsumer<MainSession2> consumer) {
		return createMainSession(TEST, start, nextId(), consumer);
	}
	
	static MainSessionCallback createMainSession(MainSessionType type, Instant start, String uuid, SafeConsumer<MainSession2> consumer) {
		var main = new MainSession2(uuid, start, threadName(), type.name());
		return completeAndEmit(main, consumer).createCallback();
	}

	public static LocalRequestCallback createLocalRequest(Instant start, SafeConsumer<LocalRequest2> consumer) {
		var req = new LocalRequest2(nextId(), requireSessionIdFor(LOCAL), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
	}
	
	public static DatabaseRequestCallback createDatabaseRequest(Instant start, SafeConsumer<DatabaseRequest2> consumer) {
		var req = new DatabaseRequest2(nextId(), requireSessionIdFor(JDBC), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
	}
	
	public static HttpRequestCallback createHttpRequest(Instant start, String rid, SafeConsumer<HttpRequest2> consumer) {
		var req = new HttpRequest2(rid, requireSessionIdFor(REST), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
	}

	public static FtpRequestCallback createFtpRequest(Instant start, SafeConsumer<FtpRequest2> consumer) {
		var req = new FtpRequest2(nextId(), requireSessionIdFor(FTP), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
	}
	
	public static MailRequestCallback createMailRequest(Instant start, SafeConsumer<MailRequest2> consumer) {
		var req = new MailRequest2(nextId(), requireSessionIdFor(SMTP), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
	}

	public static DirectoryRequestCallback createNamingRequest(Instant start, SafeConsumer<DirectoryRequest2> consumer) {
		var req = new DirectoryRequest2(nextId(), requireSessionIdFor(LDAP), start, threadName());
		return completeAndEmit(req, consumer).createCallback();
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
	
	static <T extends EventTrace> T completeAndEmit(T req, SafeConsumer<T> consumer) {
		try {
			consumer.accept(req);
		}
		catch (Throwable e) {
			//TODO report error
		}
		finally {
			context().emitTrace(req);
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
