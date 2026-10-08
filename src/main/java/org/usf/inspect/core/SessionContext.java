package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.time.Duration.between;
import static java.util.Objects.nonNull;
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
	
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger requestMask = new AtomicInteger(); // thread safe

	@Getter
	private final AbstractSessionUpdate session;
	
	public boolean updateEventMask(SessionEventMask mask) {
		return !mask.is(requestMask.getAndUpdate(v-> {
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
				var drt = session.getAsyncDuration();
				if(drt == -1) {
					var now = systemUTC().instant();
					session.setAsyncDuration(between(session.getEnd(), now).toMillis());
					hub().emitTrace(new SessionAsyncDurationUpdate(session.getId(), session instanceof MainSessionUpdate, drt));
				}
				else {
					hub().emitReport("SessionContext.threadCountDown", "");
				}
//				System.err.println(session);
			}
			return v;
		});
	}

	public boolean wasCompleted() {
		return nonNull(session.getEnd()) && threadCount.get() == 0;
	}
	
	public boolean isStartup() {
		return session.isStartup();
	}
	
	public void updateAsync() {
		threadCount.updateAndGet(v->{
			if(v > 0) {
				if(nonNull(session.getEnd())) {
					session.setAsyncDuration(-1L);
				}
				else {
					hub().emitReport("SessionContext.updateAsync", "");
				}
			}
			return v;
		});
	}
}
