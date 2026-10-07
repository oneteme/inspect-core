package org.usf.inspect.core;

import static java.util.Objects.nonNull;

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
	
	public void threadCountDown() { //TODO emit update
		threadCount.decrementAndGet();
	}

	public boolean wasCompleted() {
		return nonNull(session.getEnd()) && threadCount.get() == 0;
	}

	public boolean isAsync() {
		return nonNull(session.getEnd()) && threadCount.get() > 0;
	}
	
	public boolean isStartup() {
		return session.isStartup();
	}
}
