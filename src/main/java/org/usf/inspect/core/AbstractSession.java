package org.usf.inspect.core;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.usf.inspect.core.ErrorReporter.reportMessage;
import static org.usf.inspect.core.SessionContext.context;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public abstract class AbstractSession implements CompletableMetric {
	
	private final AtomicInteger async;
	private int requestsMask;
	
	AbstractSession() {
		this.async = new AtomicInteger(0);
	}

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
		this.async = new AtomicInteger(req.getAsync().get());
	}
	
	void countUp() {
		runSynchronized(async::incrementAndGet); //avoid change while cloning
	}
	
	void countDown() {
		runSynchronized(async::decrementAndGet); //avoid change while cloning
	}
	
	@Override
	public boolean wasCompleted() {
		var c = async.get();
		if(c < 0) {
			reportMessage("wasCompleted", this, "tasks<0");
			c=0;
		}
		return CompletableMetric.super.wasCompleted() && c==0;
	}

	public void updateRequestsMask(RequestMask mask) {
		this.requestsMask |= mask.getValue();
	}
	
	public SessionContext createContext() {
		return createContext(1, SECONDS);
	}
	
	public SessionContext createContext(long delay, TimeUnit unit) {
		return context(10+unit.toMillis(delay), this); //10ms safety margin
	}
	
	public abstract String getInstanceId(); //server usage 
	
	public abstract void setInstanceId(String id);
	
	public abstract AbstractSession updateContext();
	
	public abstract AbstractSession releaseContext();
}
