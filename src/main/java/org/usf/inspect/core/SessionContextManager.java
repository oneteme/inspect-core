package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
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
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.usf.inspect.core.LogEntry.Level;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Manages the per-thread trace session context used to associate requests and log entries
 * with the currently active session, and provides factory methods for creating all session and request signal types.
 *
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionContextManager {

	private static final ThreadLocal<AbstractSessionUpdate> localTrace = new ThreadLocal<>(); //replaceable ScopedValue in Java 21+
	private static AbstractSessionUpdate startupContext; //avoid ThreadLocal for startup context

    /**
     * Wraps the given runnable so that it executes within the current session context,
     * keeping the active thread count consistent.
     *
     * @param cmd the runnable to wrap
     * @return the wrapped runnable, or the original when no active context exists
     */
    public static Runnable aroundRunnable(Runnable cmd) {
    	var ses = activeContext(); //do not use requireActiveContext
    	if(nonNull(ses)) {
    		ses.threadCountUp();
    		return ()-> runWithContext(ses, cmd, ses::threadCountDown);
    	}
		return cmd;
    }
    
    /**
     * Wraps the given callable so that it executes within the current session context,
     * keeping the active thread count consistent.
     *
     * @param <T> the callable result type
     * @param cmd the callable to wrap
     * @return the wrapped callable, or the original when no active context exists
     */
    public static <T> Callable<T> aroundCallable(Callable<T> cmd) {
    	var ses = activeContext(); //do not use requireActiveContext
    	if(nonNull(ses)) {
    		ses.threadCountUp();
    		return ()-> callWithContext(ses, cmd::call, ses::threadCountDown);
    	}
		return cmd;
    }
    
    /**
     * Wraps the given supplier so that it executes within the current session context,
     * keeping the active thread count consistent.
     *
     * @param <T> the supplier result type
     * @param cmd the supplier to wrap
     * @return the wrapped supplier, or the original when no active context exists
     */
    public static <T> Supplier<T> aroundSupplier(Supplier<T> cmd) {
    	var ses = activeContext(); //do not use requireActiveContext
    	if(nonNull(ses)) {
    		ses.threadCountUp();
    		return ()-> callWithContext(ses, cmd::get, ses::threadCountDown);
    	}
		return cmd;
    }

    /**
     * Executes the given runnable within the supplied session context, then runs the finalizer.
     *
     * @param ctx the session context to activate for the duration of the runnable
     * @param cmd the runnable to execute
     * @param finalize the action to run after the command completes (may be {@code null})
     */
    public static void runWithContext(AbstractSessionUpdate ctx, Runnable cmd, Runnable finalize) {
    	callWithContext(ctx, ()-> { cmd.run(); return null;}, finalize);
    }
    
    /**
     * Executes the given callable within the supplied session context, then runs the finalizer.
     *
     * @param <T> the callable result type
     * @param <E> the declared exception type
     * @param ctx the session context to activate for the duration of the call
     * @param call the callable to execute
     * @param finalize the action to run after the call completes (may be {@code null})
     * @return the result of the callable
     * @throws E if the callable throws a checked exception
     */
    public static <T, E extends Exception> T callWithContext(AbstractSessionUpdate ctx, SafeCallable<T, E> call, Runnable finalize) throws E {
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
			if(nonNull(finalize)) {
				finalize.run();
			}
		}	
	}
    
	/**
	 * Returns the current active session context, reporting an error when none is present or the context is already completed.
	 *
	 * @return the active session context, or {@code null} when none could be resolved
	 */
	public static AbstractSessionUpdate requireActiveContext() {
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

	/**
	 * Returns the current active session context without validation, preferring the thread-local context over the startup context.
	 *
	 * @return the active session context, or {@code null} when none is set
	 */
	public static AbstractSessionUpdate activeContext() {
		var trc = localTrace.get();
		return nonNull(trc) ? trc : startupContext; // priority
	}

	/**
	 * Registers the given session as the active context for the current thread (or as the global startup context).
	 *
	 * @param session the session to activate
	 */
	public static void setActiveContext(AbstractSessionUpdate session) {
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
	
	/**
	 * Clears the given session from the active context for the current thread (or from the global startup context).
	 *
	 * @param ctx the session to deactivate
	 */
	public static void clearContext(AbstractSessionUpdate ctx) {
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

	/**
	 * Creates a new HTTP session signal starting at the given instant.
	 *
	 * @param start the session start time
	 * @param uuid the session identifier, or {@code null} to generate one automatically
	 * @return the new HTTP session signal
	 */
	public static HttpSessionSignal createHttpSession(Instant start, String uuid) {
		var ses = new HttpSessionSignal(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
		ses.setLinked(nonNull(uuid));
		return ses;
	}

	static MainSessionSignal createStartupSession(Instant start, String uuid) {
		return createMainSession(STARTUP, start, requireNonNullElseGet(uuid, SessionContextManager::nextId));
	}

	/**
	 * Creates a new BATCH-type main session signal starting at the given instant.
	 *
	 * @param start the session start time
	 * @return the new main session signal
	 */
	public static MainSessionSignal createBatchSession(Instant start) {
		return createMainSession(BATCH, start, nextId());
	}
	
	/**
	 * Creates a new TEST-type main session signal starting at the given instant.
	 *
	 * @param start the session start time
	 * @return the new main session signal
	 */
	public static MainSessionSignal createTestSession(Instant start) {
		return createMainSession(TEST, start, nextId());
	}
	
	static MainSessionSignal createMainSession(MainSessionType type, Instant start, String uuid) {
		return new MainSessionSignal(uuid, start, threadName(), type.name());
	}

	/**
	 * Creates a new local (in-process) request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @return the new local request signal
	 */
	public static LocalRequestSignal createLocalRequest(Instant start) {
		return new LocalRequestSignal(nextId(), requireSessionIdFor(LOCAL), start, threadName());
	}
	
	/**
	 * Creates a new database (JDBC) request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @return the new database request signal
	 */
	public static DatabaseRequestSignal createDatabaseRequest(Instant start) {
		return new DatabaseRequestSignal(nextId(), requireSessionIdFor(JDBC), start, threadName());
	}
	
	/**
	 * Creates a new HTTP (REST) request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @param rid the request identifier to link the outgoing request to its remote session
	 * @return the new HTTP request signal
	 */
	public static HttpRequestSignal createHttpRequest(Instant start, String rid) {
		return new HttpRequestSignal(rid, requireSessionIdFor(REST), start, threadName());
	}

	/**
	 * Creates a new FTP request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @return the new FTP request signal
	 */
	public static FtpRequestSignal createFtpRequest(Instant start) {
		return new FtpRequestSignal(nextId(), requireSessionIdFor(FTP), start, threadName());
	}
	
	/**
	 * Creates a new SMTP mail request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @return the new mail request signal
	 */
	public static MailRequestSignal createMailRequest(Instant start) {
		return new MailRequestSignal(nextId(), requireSessionIdFor(SMTP), start, threadName());
	}

	/**
	 * Creates a new LDAP directory request signal for the active session, starting at the given instant.
	 *
	 * @param start the request start time
	 * @return the new directory request signal
	 */
	public static DirectoryRequestSignal createNamingRequest(Instant start) {
		return new DirectoryRequestSignal(nextId(), requireSessionIdFor(LDAP), start, threadName());
	}
	
	static String requireSessionIdFor(RequestMask mask) {
		var ses = requireActiveContext();
		if(nonNull(ses)) {
			if(ses.updateMask(mask)) {
				hub().emitTrace(new SessionMaskUpdate(ses.getId(), ses instanceof MainSessionUpdate, ses.getRequestMask().get()));
			}
			return ses.getId();
		}
		return null;
	}
	
	/**
	 * Emits an INFO-level log entry attached to the active session context.
	 *
	 * @param msg the message to log
	 */
	public static void emitInfo(String msg) {
		emitLog(INFO, msg);
	}

	/**
	 * Emits a WARN-level log entry attached to the active session context.
	 *
	 * @param msg the message to log
	 */
	public static void emitWarn(String msg) {
		emitLog(WARN, msg);
	}

	/**
	 * Emits an ERROR-level log entry attached to the active session context.
	 *
	 * @param msg the message to log
	 */
	public static void emitError(String msg) {
		emitLog(ERROR, msg);
	}

	private static void emitLog(Level lvl, String msg) {
		var log = logEntry(lvl, msg); // no stack
		var ctx = requireActiveContext();
		if(nonNull(ctx)) {
			log.setSessionId(ctx.getId());
		}
		hub().emitTrace(log);
	}

	/**
	 * Generates a new random UUID string to use as a trace identifier.
	 *
	 * @return a new random UUID string
	 */
	public static String nextId() {
		return randomUUID().toString();
	}

	static void reportNoActiveContext(String action) {
		hub().reportMessage(true, action, "no active context");
	}
	
	static void reportContextConflict(String action, String prev, String next) {
		hub().reportMessage(true, action, format("previous=%s, next=%s", prev, next));
	}

	static void reportIllegalContextState(String action, String msg) {
		hub().reportMessage(true, action, msg);
	}
}
