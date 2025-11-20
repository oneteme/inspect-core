package org.usf.inspect.core;

import java.util.concurrent.ScheduledFuture;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class ScheduledFutureWrapper<V> extends FutureWrapper<V> implements ScheduledFuture<V> {
	
	ScheduledFutureWrapper(ScheduledFuture<V> sf, Runnable onCancel) {
		super(sf, onCancel);
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return super.cancel(mayInterruptIfRunning);
	}
	
	@Delegate
	ScheduledFuture<V> sf(){
		return (ScheduledFuture<V>) future;
	}
}
