package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.log;

import java.util.concurrent.atomic.AtomicInteger;
abstract class AbstractSession implements Session {

	private final AtomicInteger pending = new AtomicInteger();

	@Override
	public void lock(){ //must be called before session end
		pending.incrementAndGet();
	}

	@Override
	public void unlock() {
		pending.decrementAndGet();
	}

	@Override
	public boolean wasCompleted() {
		var c = pending.get();
		if(c < 0) {
			log.warn("illegal session lock state={}, {}", c, this);
			return true;
		}
		return c == 0 && nonNull(getEnd());
	}
}
