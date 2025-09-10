package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;

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

	private final AtomicInteger threads = new AtomicInteger();
	private int requestsMask;
	private String instanceId; //server usage 

	AbstractSession() { }

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
	}

	public void lock(){ //must be called before session end
		threads.incrementAndGet();
	}

	public void unlock() {
		threads.decrementAndGet();
	}

	@Override
	public boolean wasCompleted() {
		var c = threads.get();
		if(c < 0) {
			reporter().action("Session.wasCompleted").message("threads="+c).trace(this).emit();
			return true;
		}
		return c == 0 && nonNull(getEnd());
	}

	public void updateRequestsMask(RequestMask mask) {
		this.requestsMask |= mask.getValue();
	}

	public abstract AbstractSession updateContext();
	
	public abstract AbstractSession releaseContext();
}
