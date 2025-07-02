package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractSession implements Session {

	private final AtomicInteger pending = new AtomicInteger();
	
	//v1.1
	private List<Trace> traces;
	private List<ExceptionInfo> exceptions;
	
	public boolean appendException(ExceptionInfo e) {
		if(isNull(exceptions)) {
			exceptions = new ArrayList<>();
		}
		return exceptions.add(e);
	}

	@Override
	public void lock(){ //must be called before session end
		pending.incrementAndGet();
	}

	@Override
	public void unlock() {
		pending.decrementAndGet();
	}

	@Override
	public boolean isCompleted() {
		var c = pending.get();
		if(c < 0) {
			log.warn("illegal session lock state={}, {}", c, this);
			return true;
		}
		return c == 0 && nonNull(getEnd());
	}
}
