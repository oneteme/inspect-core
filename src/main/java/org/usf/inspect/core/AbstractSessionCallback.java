package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractSessionCallback implements EventTrace {

	private final String id;
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger requestMask = new AtomicInteger(); // thread safe
	private boolean async;
	private Instant end;
	@Setter private ExceptionInfo exception;

	void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	void threadCountDown() {
		threadCount.decrementAndGet();
	}
	
	public void setEnd(Instant end){
		this.end = end;
		this.async = threadCount.get() == 0;
	}
	
	public boolean updateMask(RequestMask mask) {
		return !mask.is(requestMask.getAndUpdate(v-> v|mask.getValue()));
	}
	
	public boolean wasCompleted() {
		return nonNull(getEnd()) && !async;
	}
}
