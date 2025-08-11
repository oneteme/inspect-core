package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public abstract class AbstractSession implements Session {

	private final AtomicInteger threads = new AtomicInteger();
	private int requestsMask; //TD -1 if absent
	private String instanceId; //server usage 
	
	@JsonCreator AbstractSession() { }

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
	}
	
	@Override
	public void lock(){ //must be called before session end
		threads.incrementAndGet();
	}

	@Override
	public void unlock() {
		threads.decrementAndGet();
	}

	@Override
	public boolean wasCompleted() {
		var c = threads.get();
		if(c < 0) {
			context().reportError(format("illegal session thread state=%d, id=%s", c, getId()));
			return true;
		}
		return c == 0 && nonNull(getEnd());
	}
	
	@Override
	public void updateRequestsMask(RequestMask mask) {
		this.requestsMask |= mask.getValue();
	}
	
	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}
