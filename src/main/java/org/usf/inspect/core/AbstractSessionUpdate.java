package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.formatLocation;
import static org.usf.inspect.core.RequestMask.ASYNC;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public abstract class AbstractSessionUpdate implements TraceUpdate, AtomicTrace {

	private final String id;
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger requestMask = new AtomicInteger(); // thread safe
	private Instant end;
	@Setter private Instant start;
	@Setter private String name; //title, topic
	@Setter private String user;
	@Setter private String location; //class.method, URL, endpoint
	@Setter private ExceptionInfo exception; //TD trace exception separately
	
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}
	
	public void setEnd(Instant end){
		if(threadCount.get() > 0) {
			requestMask.updateAndGet(v-> v | ASYNC.getValue());
		}
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

	@JsonIgnore
	public boolean isAsync() {
		return (requestMask.get() & ASYNC.getValue()) == ASYNC.getValue();
	}
	
	@JsonIgnore
	public boolean isStartup() {
		return false;
	}
}
