package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionEventMask.EVENT;
import static org.usf.inspect.core.SessionEventMask.FTP;
import static org.usf.inspect.core.SessionEventMask.JDBC;
import static org.usf.inspect.core.SessionEventMask.LDAP;
import static org.usf.inspect.core.SessionEventMask.LOCAL;
import static org.usf.inspect.core.SessionEventMask.REST;
import static org.usf.inspect.core.SessionEventMask.SMTP;

import java.time.Instant;
import java.util.UUID;

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
    
	private static final ContextPropagator EMPTY_CONTEXT_PROPAGATOR = ()->{};
	
	public static ContextPropagator contextPropagator(SessionContext ctx, boolean updateThreadCount, String action) {
		if(!assertActiveContext(ctx, action)) {
			return EMPTY_CONTEXT_PROPAGATOR;
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
		if(nonNull(ctx) && ctx.isFullyCompleted()) {
			ctx = null;
		}
		return ctx;
	}
	
	static void setActiveContext(AbstractSessionUpdate session, boolean startup) {
		if(startup) {
			if(isNull(session) || nonNull(session.getEnd())) {
				REPORTER.warn("SessionContextManager.setActiveContext: startup session context is null or already completed");
			}
			else if(isNull(startupContext)) {
				startupContext = new SessionContext(session, startup);
			}
			else if(startupContext.getSession() == session) {
				REPORTER.warn("SessionContextManager.setActiveContext: startup context already set for session {}", 
						session.getId());
			}
			else {
				REPORTER.warn("SessionContextManager.setActiveContext: cannot override startup context {} with {}", 
						startupContext.getSession().getId(), session.getId());
			}
		}
		else {
			setActiveContext(session);
		}
	}

	public static void setActiveContext(AbstractSessionUpdate session) {
		if(isNull(session) || nonNull(session.getEnd())) {
			REPORTER.warn("SessionContextManager.setActiveContext: session context is null or already completed");
		}
		else {
			var ctx = localContext.get();
			if(isNull(ctx)) {
				localContext.set(new SessionContext(session, false));
			}
			else if(ctx.getSession() != session) {
				if(nonNull(ctx.isFullyCompleted())) {
					REPORTER.warn("SessionContextManager.setActiveContext: current context is not completed {} and will be overridden with {}",
							ctx.getSession().getId(), session.getId());
				}
				localContext.set(new SessionContext(session, false));
			}// else same context session
		}
	}
	
	static SessionContext clearContext(AbstractSessionUpdate session, boolean startup) {
		if(startup) {
			if(isNull(session)) {
				REPORTER.warn("SessionContextManager.clearContext: session cannot be null");
				return null;
			}
			var ctx = startupContext;
			if(isNull(ctx)) {
				REPORTER.warn("SessionContextManager.clearContext: startup context is null");
			}
			else if(ctx.getSession() == session) {
				startupContext = null;
				return ctx;
			}
			else {
				REPORTER.warn("SessionContextManager.clearContext: cannot clear startup context {} with {}", 
						ctx.getSession().getId(), session.getId());
			}
			return null;
		}
		return clearContext(session);
	}
	
	public static SessionContext clearContext(AbstractSessionUpdate session) {
		if(isNull(session)) {
			REPORTER.warn("SessionContextManager.clearContext: session cannot be null");
			return null;
		}
		var ctx = localContext.get();
		if(isNull(ctx)) {
			REPORTER.warn("SessionContextManager.clearContext: session context is null");
		}
		else if(ctx.getSession() == session) {
			localContext.remove();  //even if !complete
			return ctx;
		}
		else {
			REPORTER.warn("SessionContextManager.clearContext: cannot clear context {} with {}", 
					ctx.getSession().getId(), session.getId());
		}
		return null;
	}

	public static HttpSessionSignal createHttpSession(Instant start, UUID uuid) { // HttpRequest.id
		var sgn = new HttpSessionSignal(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
		sgn.setLinked(nonNull(uuid));
		return sgn;
	}
	
	public static MainSessionSignal createMainSession(MainSessionType type, Instant start) {
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

	public static SessionEvent createLog(Instant instant, String threadname, String lvl, String msg, String location) {
		var sid = requireSessionIdFor(EVENT, "SessionContextManager.emitLog");
		return new SessionEvent(instant, lvl, msg, threadname, location, sid);
	}
	
	static UUID requireSessionIdFor(SessionEventMask mask, String action) {
		var ctx = activeContext();
		if(assertActiveContext(ctx, action)) {
			ctx.updateEventsMask(mask);
			return ctx.getSession().getId();
		}
		return null;
	}

	public static UUID nextId() {
		return randomUUID();
	}
	
	static boolean assertActiveContext(SessionContext ctx, String action) {
		if(isNull(ctx)) {
			REPORTER.warn("{}: session context is null", action);
		}
		else if(ctx.isFullyCompleted()) {
			REPORTER.warn("{}: session context is already completed", action);
		}
		else {
			return true;
		}
		return false;
	}
	
	@FunctionalInterface
	public interface ContextPropagator extends AutoCloseable {
		
		@Override
		void close();
	}
}
