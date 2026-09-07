package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.RequestMask.ASYNC;

import java.time.Instant;
import java.util.UUID;
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
public abstract class AbstractSessionUpdate implements TraceUpdate {

	private final UUID id;
	private final AtomicInteger threadCount = new AtomicInteger(); // thread safe
	private final AtomicInteger requestMask = new AtomicInteger(); // thread safe
	private Instant end;
	@Setter private Instant start;
	@Setter private String name; //title, topic
	@Setter private String user;
	@Setter private String location; //class.method, URL, endpoint
	@Deprecated(forRemoval = true, since = "1.2")
	@Setter private ExceptionTrace exception; //trace exception separately
	
	//v1.2
	@Setter private short status; //DualEventTracer
	
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

	public void threadCountUp() {
		threadCount.incrementAndGet();
	}
	
	public void threadCountDown() {
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
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(end)
//				.withAction(command)
				.withMessageAsTopic(id.toString())
				.withStatus(getStatus()+"")
				.format();
	}
}
