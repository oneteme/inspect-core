package org.usf.inspect.http;

import static org.usf.inspect.core.SessionContextManager.runWithContext;

import org.reactivestreams.Subscription;
import org.usf.inspect.core.AbstractSessionUpdate;

import lombok.RequiredArgsConstructor;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class CoreSubscriberProxy<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<T> sub;
	private final AbstractSessionUpdate ctx;

	@Override
	public void onSubscribe(Subscription s) {
		runWithContext(ctx, ()-> sub.onSubscribe(s), ctx::threadCountUp);
	}
	
	@Override
	public void onNext(T t) {
		runWithContext(ctx, ()-> sub.onNext(t), null);
	}

	@Override
	public void onError(Throwable t) {
		runWithContext(ctx, ()-> sub.onError(t), ctx::threadCountDown);
	}

	@Override
	public void onComplete() {
		runWithContext(ctx, sub::onComplete, ctx::threadCountDown);
	}

	@Override
	public Context currentContext() {
		return sub.currentContext();
	}
	
	@Override
	public boolean equals(Object obj) {
		return sub.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return sub.hashCode();
	}
	
	@Override
	public String toString() {
		return sub.toString();
	}
}