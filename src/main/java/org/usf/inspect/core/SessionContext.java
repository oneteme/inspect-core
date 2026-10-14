package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.time.Duration.between;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.TraceHub.hub;

import java.util.concurrent.atomic.AtomicInteger;

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
	
	public boolean updateEventsMask(SessionEventMask mask) {
		return !mask.is(eventsMask.getAndUpdate(v-> {
			v = v|mask.getValue();
			session.setEventMask(v); //safe thread update
			return v;
		}));
	}

	public void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	public void threadCountDown() {
		threadCount.updateAndGet(v->{
			if(--v == 0 && nonNull(session.getAsyncDuration())) {
				if(session.getAsyncDuration() == asyncDurationInitialValue) {
					var now = systemUTC().instant();
					session.setAsyncDuration(between(session.getEnd(), now).toMillis());
					hub().emitTrace(new SessionAsyncDurationUpdate(session.getId(), session instanceof MainSessionUpdate, session.getAsyncDuration()));
				}
				else {
					REPORTER.warn("SessionContext.threadCountDown: asyncDuration already set to " + session.getAsyncDuration());
				}
			}
			return v;
		});
	}

	public boolean wasCompleted() {
		if(nonNull(session.getEnd())) {
			var drt = session.getAsyncDuration();
			return isNull(drt) || drt > asyncDurationInitialValue;
		}
		return false;
	}
	
	public void updateAsync() {
		threadCount.updateAndGet(v->{
			if(isNull(session.getEnd())) {
				REPORTER.warn("SessionContext.updateAsync: session not completed yet, end is null");
			}
			else if(v > 0) {
				session.setAsyncDuration(asyncDurationInitialValue); //initial duration
			}
			return v;
		});
	}
}
