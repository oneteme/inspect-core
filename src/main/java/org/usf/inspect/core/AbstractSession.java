package org.usf.inspect.core;

import static org.usf.inspect.core.ErrorReporter.reportMessage;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonIgnoreProperties("threads")
public abstract class AbstractSession implements CompletableMetric {
	
	private final AtomicInteger tasks;
	private int requestsMask;
	
	AbstractSession() {
		this.tasks = new AtomicInteger(0);
	}

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
		this.tasks = new AtomicInteger(req.getTasks().get());
	}
	
	void lock() {
		tasks.incrementAndGet();
	}
	
	void unlock() {
		tasks.decrementAndGet();
	}
	
	@Override
	public boolean wasCompleted() {
		var c = tasks.get();
		if(c < 0) {
			reportMessage("wasCompleted", this, "tasks<0");
			c=0;
		}
		return CompletableMetric.super.wasCompleted() && c==0;
	}

	public void updateRequestsMask(RequestMask mask) {
		this.requestsMask |= mask.getValue();
	}
	
	public abstract String getInstanceId(); //server usage 
	
	public abstract void setInstanceId(String id);
	
	public abstract AbstractSession updateContext();
	
	public abstract AbstractSession releaseContext();
}
