package org.usf.inspect.core;

import static java.lang.String.format;
import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.UUID.randomUUID;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.LogEntry.Level.ERROR;
import static org.usf.inspect.core.LogEntry.Level.INFO;
import static org.usf.inspect.core.LogEntry.Level.WARN;
import static org.usf.inspect.core.MainSessionType.SCHEDULE;
import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.MainSessionType.TEST;
import static org.usf.inspect.core.SessionMask.EVENT;
import static org.usf.inspect.core.SessionMask.FTP;
import static org.usf.inspect.core.SessionMask.JDBC;
import static org.usf.inspect.core.SessionMask.LDAP;
import static org.usf.inspect.core.SessionMask.LOCAL;
import static org.usf.inspect.core.SessionMask.REST;
import static org.usf.inspect.core.SessionMask.SMTP;
import static org.usf.inspect.core.TraceHub.hub;

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

	private static final ThreadLocal<AbstractSessionUpdate> localTrace = new ThreadLocal<>(); //replaceable ScopedValue in Java 21+
	private static AbstractSessionUpdate startupContext; //avoid ThreadLocal for startup context
    
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

	public static AbstractSessionUpdate activeContext() {
		var trc = localTrace.get();
		return nonNull(trc) ? trc : startupContext; // priority
	}

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

	public static HttpSessionSignal createHttpSession(Instant start, UUID uuid) {
		var ses = new HttpSessionSignal(requireNonNullElseGet(uuid, SessionContextManager::nextId), start, threadName());
		ses.setLinked(nonNull(uuid));
		return ses;
	}
	
	static MainSessionSignal createStartupSession(Instant start) {
		return createStartupSession(start, nextId());
	}

	static MainSessionSignal createStartupSession(Instant start, UUID uuid) {
		return createMainSession(STARTUP, start, requireNonNullElseGet(uuid, SessionContextManager::nextId));
	}

	public static MainSessionSignal createScheduleSession(Instant start) {
		return createMainSession(SCHEDULE, start, nextId());
	}
	
	public static MainSessionSignal createTestSession(Instant start) {
		return createMainSession(TEST, start, nextId());
	}
	
	static MainSessionSignal createMainSession(MainSessionType type, Instant start, UUID uuid) {
		return new MainSessionSignal(uuid, start, threadName(), type.name());
	}

	public static LocalRequestSignal createLocalRequest(Instant start) {
		return createLocalRequest(start, nextId());
	}

	public static LocalRequestSignal createLocalRequest(Instant start, UUID rid) {
		return new LocalRequestSignal(rid, requireSessionIdFor(LOCAL), start, threadName());
	}
	
	public static DatabaseRequestSignal createDatabaseSignal(Instant start) {
		return new DatabaseRequestSignal(nextId(), requireSessionIdFor(JDBC), start, threadName());
	}
	
	public static HttpRequestSignal createHttpRequest(Instant start, UUID rid) {
		return new HttpRequestSignal(rid, requireSessionIdFor(REST), start, threadName());
	}

	public static FtpRequestSignal createFtpSignal(Instant start) {
		return new FtpRequestSignal(nextId(), requireSessionIdFor(FTP), start, threadName());
	}
	
	public static MailRequestSignal createMailSignal(Instant start) {
		return new MailRequestSignal(nextId(), requireSessionIdFor(SMTP), start, threadName());
	}

	public static DirectoryRequestSignal createNamingSignal(Instant start) {
		return new DirectoryRequestSignal(nextId(), requireSessionIdFor(LDAP), start, threadName());
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
	
	public static void emitLog(LogEntry.Level lvl, String msg) {
		var evt = new SessionEvent(systemUTC().instant(), 
				lvl.name(), msg, null, requireSessionIdFor(EVENT));
		hub().emitTrace(evt);
	}
	
	static UUID requireSessionIdFor(SessionMask mask) {
		var ses = requireActiveContext();
		if(nonNull(ses)) {
			if(ses.updateMask(mask)) {
				var upd = new SessionMaskUpdate(ses.getId(), ses instanceof MainSessionUpdate, ses.getRequestMask().get());
				hub().emitTrace(upd);
			}
			return ses.getId();
		}
		return null;
	}

	public static UUID nextId() {
		return randomUUID();
	}

	static void reportNoActiveContext(String action) {
		hub().reportMessage(action, "no active context");
	}
	
	static void reportContextConflict(String action, UUID prev, UUID next) {
		hub().reportMessage(action, format("previous=%s, next=%s", prev, next));
	}

	static void reportIllegalContextState(String action, String msg) {
		hub().reportMessage(action, msg);
	}
}
