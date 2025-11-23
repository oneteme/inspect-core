package org.usf.inspect.core;

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
	
	private final AtomicInteger threadCount;
	private boolean async;
	private int requestsMask;
	
	AbstractSession() {
		this.threadCount = new AtomicInteger(0);
	}

	AbstractSession(AbstractSession req) {
		this.threadCount = new AtomicInteger(req.threadCount.get());
		this.async = req.async;
		this.requestsMask = req.requestsMask;
	}
	
	void threadCountUp() {
		runSynchronized(threadCount::incrementAndGet); //avoid change while cloning
	}
	
	void threadCountDown() {
		runSynchronized(()->{
			threadCount.decrementAndGet();
			if(wasCompleted() && async) {
				emit();
			}
		});
	}
	
	public void updateRequestsMask(RequestMask mask) {
		runSynchronized(()-> this.requestsMask |= mask.getValue());
	}
	
	public abstract String getInstanceId(); //server usage 
	
	public abstract void setInstanceId(String id);
	
	public abstract AbstractSession updateContext();
	
	public abstract AbstractSession releaseContext();
}
