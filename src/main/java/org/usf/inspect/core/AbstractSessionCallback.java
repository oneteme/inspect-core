package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.formatLocation;

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
public abstract class AbstractSessionCallback implements Callback {

	private final String id;
	private final boolean startup;
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger requestMask = new AtomicInteger(); // thread safe
	private boolean async;
	private Instant end;
	@Setter private String name; //title, topic
	@Setter private String user;
	@Setter private String location; //class.method, URL
	@Setter private ExceptionInfo exception;
	
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}
	
	public void setEnd(Instant end){
		this.async = threadCount.get() > 0; //session ended but some threads are still running
		this.end = end;
	}
	
	@Deprecated(forRemoval = true, since = "1.1")
	public void setRequestMask(int mask) {
		requestMask.set(mask);
	}
	
	public boolean updateMask(RequestMask mask) {
		return !mask.is(requestMask.getAndUpdate(v-> v|mask.getValue()));
	}

	void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	void threadCountDown() {
		threadCount.decrementAndGet();
	}

	public boolean wasCompleted() {
		return nonNull(getEnd()) && !isAsync();
	}
}
