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
 * Base update that tracks the lifecycle of a traced session.
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
	
	/**
	 * Sets the formatted location for this session update.
	 *
	 * @param className the declaring class name
	 * @param methodName the declaring method name
	 */
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}
	
	/**
	 * Marks the session as ended and flags it as asynchronous when child threads are still active.
	 *
	 * @param end the session end time
	 */
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

	/**
	 * Adds the given mask to the current request mask.
	 *
	 * @param mask the mask to apply
	 * @return {@code true} if the mask was not already set
	 */
	public boolean updateMask(RequestMask mask) {
		return !mask.is(requestMask.getAndUpdate(v-> v|mask.getValue()));
	}

	/**
	 * Increments the number of active child threads.
	 */
	public void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	/**
	 * Decrements the number of active child threads.
	 */
	public void threadCountDown() {
		threadCount.decrementAndGet();
	}

	/**
	 * Indicates whether the session completed synchronously.
	 *
	 * @return {@code true} when the session has an end time and is not asynchronous
	 */
	public boolean wasCompleted() {
		return nonNull(getEnd()) && !isAsync();
	}

	/**
	 * Indicates whether the session was marked as asynchronous.
	 *
	 * @return {@code true} when the asynchronous request mask is set
	 */
	@JsonIgnore
	public boolean isAsync() {
		return (requestMask.get() & ASYNC.getValue()) == ASYNC.getValue();
	}
	
	/**
	 * Indicates whether this session update represents startup activity.
	 *
	 * @return {@code false} by default
	 */
	@JsonIgnore
	public boolean isStartup() {
		return false;
	}
}
