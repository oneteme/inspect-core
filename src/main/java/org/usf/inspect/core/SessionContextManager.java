package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionEvent.LogLevel.ERROR;
import static org.usf.inspect.core.SessionEvent.LogLevel.INFO;
import static org.usf.inspect.core.SessionEvent.LogLevel.WARN;
import static org.usf.inspect.core.SessionEventMask.EVENT;
import static org.usf.inspect.core.SessionEventMask.FTP;
import static org.usf.inspect.core.SessionEventMask.JDBC;
import static org.usf.inspect.core.SessionEventMask.LDAP;
import static org.usf.inspect.core.SessionEventMask.LOCAL;
import static org.usf.inspect.core.SessionEventMask.REST;
import static org.usf.inspect.core.SessionEventMask.SMTP;
import static org.usf.inspect.core.TraceHub.hub;

import java.time.Instant;
import java.util.UUID;

import org.usf.inspect.core.SessionEvent.LogLevel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionContextManager {

	private static final ThreadLocal<SessionContext> localContext = new ThreadLocal<>(); //replaceable ScopedValue in Java 21+
	private static SessionContext startupContext; //avoid ThreadLocal for startup context
    
	public static ContextPropagator contextPropagator(SessionContext ctx, boolean updateThreadCount, String action) {
		if(isNull(ctx) || ctx.wasCompleted()) {
			reportNullOrCompletedContext(action);
			return ()-> {};
		}
    	var prv = activeContext();
    	if(ctx != prv) {
    		localContext.set(ctx);
		}
    	if(updateThreadCount) {
    		ctx.threadCountUp();
    	}
    	return ()-> {
        	if(updateThreadCount) {
        		ctx.threadCountDown();
        	}
    		if(prv != ctx) {
    			if(nonNull(prv)) {
    	    		localContext.set(prv);
    			}
    			else {
    				localContext.remove();
    			}
    		}
    	};
    }

	public static SessionContext activeContext() {
		var ctx = localContext.get();
		if(isNull(ctx)) {
			ctx = startupContext;
		}
		if(nonNull(ctx) && ctx.wasCompleted()) {
			ctx = null;
		}
		return ctx;
	}

	public static void setActiveContext(AbstractSessionUpdate session) {
		if(isNull(session) || nonNull(session.getEnd())) {
			reportNullOrCompletedContext("setActiveContext");
		}
		else if(session.isStartup()) {
			if(isNull(startupContext)) {
				startupContext = new SessionContext(session);
			}
			else if(startupContext.getSession() == session) {
				hub().emitReport("setActiveContext", "startup context already set");
			}
			else {
				reportContextConflict("setActiveContext", startupContext.getSession().getId(), session.getId());
			}
		}
		else {
			var ctx = localContext.get();
			if(nonNull(ctx) && !ctx.wasCompleted()) {
				hub().emitReport("setActiveContext", "current context is not completed " + ctx.getSession().getId());
			}
			localContext.set(new SessionContext(session));
		}
	}
	
	public static void clearContext(AbstractSessionUpdate session) {
		if(isNull(session)) {
			hub().emitReport("clearContext", "session cannot be null");
			return;
		}
		if(session.isStartup()) {
			if(isNull(startupContext)) {
				hub().emitReport("clearContext", "");
			}
			if(startupContext.getSession() == session) {
				startupContext = null;
			}
			else {
				reportContextConflict("clearContext", startupContext.getSession().getId(), session.getId());
			}
		}
		else {
			var ctx = localContext.get();
			if(isNull(ctx)) {
				hub().emitReport("clearContext", "");
			}
			else if(ctx.getSession() == session) {
				localContext.remove();  //even if !complete
			}
			else {
				reportContextConflict("clearContext", ctx.getSession().getId(), session.getId());
			}
		}
	}

	public static HttpSessionSignal createHttpSession(Instant start, UUID uuid) { // HttpRequest.id
		var sgn = new HttpSessionSignal(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
		sgn.setLinked(nonNull(uuid));
		return sgn;
	}
	
	static MainSessionSignal createMainSession(MainSessionType type, Instant start) {
		return new MainSessionSignal(nextId(), start, threadName(), type.name());
	}
	
	public static LocalRequestSignal createLocalRequest(Instant start) {
		var sid = requireSessionIdFor(LOCAL, "SessionContextManager.createLocalRequest");
		return new LocalRequestSignal(nextId(), sid, start, threadName());
	}
	
	public static DatabaseRequestSignal createDatabaseSignal(Instant start) {
		var sid = requireSessionIdFor(JDBC, "SessionContextManager.createDatabaseSignal");
		return new DatabaseRequestSignal(nextId(), sid, start, threadName());
	}
	
	public static HttpRequestSignal createHttpRequest(Instant start, UUID rid) {
		var sid = requireSessionIdFor(REST, "SessionContextManager.createHttpRequest");
		return new HttpRequestSignal(rid, sid, start, threadName());
	}

	public static FtpRequestSignal createFtpSignal(Instant start) {
		var sid = requireSessionIdFor(FTP, "SessionContextManager.createFtpSignal");
		return new FtpRequestSignal(nextId(), sid, start, threadName());
	}
	
	public static MailRequestSignal createMailSignal(Instant start) {
		var sid = requireSessionIdFor(SMTP, "SessionContextManager.createMailSignal");
		return new MailRequestSignal(nextId(), sid, start, threadName());
	}

	public static DirectoryRequestSignal createNamingSignal(Instant start) {
		var sid = requireSessionIdFor(LDAP, "SessionContextManager.createNamingSignal");
		return new DirectoryRequestSignal(nextId(), sid, start, threadName());
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
	
	public static void emitLog(LogLevel lvl, String msg) {
		var sid = requireSessionIdFor(EVENT, "SessionContextManager.emitLog");
		var evt = new SessionEvent(systemUTC().instant(), lvl.name(), msg, null, sid);
		hub().emitTrace(evt);
	}
	
	static UUID requireSessionIdFor(SessionEventMask mask, String action) {
		var ctx = activeContext();
		if(nonNull(ctx)) {
			var ses = ctx.getSession();
			if(ctx.updateEventMask(mask)) {
				var upd = new SessionMaskUpdate(ses.getId(), ses instanceof MainSessionUpdate, ctx.getSession().getEventMask());
				hub().emitTrace(upd);
			}
			return ses.getId();
		}
		reportNullOrCompletedContext(action);
		return null;
	}

	public static UUID nextId() {
		return randomUUID();
	}
	
	static void reportNullOrCompletedContext(String action) {
		hub().emitReport(action, "session context is null or already completed");
	}

	static void reportContextConflict(String action, UUID prev, UUID next) {
		hub().emitReport(action, format("session context conflict : previous=%s, next=%s", prev, next));
	}

	@FunctionalInterface
	public interface ContextPropagator extends AutoCloseable {
		
		@Override
		void close();
	}
}
