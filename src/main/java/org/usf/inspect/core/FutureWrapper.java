package org.usf.inspect.core;

import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
public class FutureWrapper<V> implements Future<V>  {
	
	@Delegate
	final Future<V> future;
	private final Runnable onCancel;
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if(future.cancel(mayInterruptIfRunning)) {
			onCancel.run();
			return true;
		}
		return false;
	}
}
