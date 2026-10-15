package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.time.Duration.between;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.TraceHub.hub;

import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.AbstractSessionUpdate;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.SessionAsyncDurationUpdate;
import org.usf.inspect.SessionEventMask;
import org.usf.inspect.SessionMaskUpdate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class SessionContext {
	
	static final long asyncDurationInitialValue = -1L;
	
	@Getter	private final AbstractSessionUpdate session;
	@Getter	private final boolean startup;
	
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger eventsMask = new AtomicInteger(); // thread safe
	
	public void updateEventsMask(SessionEventMask mask) {
		var prv = eventsMask.getAndUpdate(v-> v|mask.getValue()); 
		if(!mask.is(prv)) {
			session.setEventMask(eventsMask.get());
			hub().emitTrace(new SessionMaskUpdate(session.getId(), session instanceof MainSessionUpdate, session.getEventMask()));
		}
	}

	public void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	public void threadCountDown() {
		var v = threadCount.decrementAndGet();
		if(v == 0 && nonNull(session.getAsyncDuration())) {
			if(session.getAsyncDuration() == asyncDurationInitialValue) {
				var now = systemUTC().instant();
				session.setAsyncDuration(between(session.getEnd(), now).toMillis());
				hub().emitTrace(new SessionAsyncDurationUpdate(session.getId(), session instanceof MainSessionUpdate, session.getAsyncDuration()));
			}
			else {
				REPORTER.warn("SessionContext.threadCountDown: asyncDuration already set to {}", session.getAsyncDuration());
			}
		}
		else if(v < 0) {
			REPORTER.warn("SessionContext.threadCountDown: threadCount is negative {}", v);
		}
	}

	public boolean isFullyCompleted() {
		if(nonNull(session.getEnd())) {
			var drt = session.getAsyncDuration();
			return isNull(drt) || drt > asyncDurationInitialValue;
		}
		return false;
	}
	
	public void checkPendingAsync() {
		if(isNull(session.getEnd())) {
			REPORTER.warn("SessionContext.updateAsync: session end is null");
		}
		if(nonNull(session.getAsyncDuration())) {
			REPORTER.warn("SessionContext.updateAsync: asyncDuration already set to {}", session.getAsyncDuration());
		}
		else if(threadCount.get() > 0) {
			session.setAsyncDuration(asyncDurationInitialValue); //initial duration
		}
	}
}
