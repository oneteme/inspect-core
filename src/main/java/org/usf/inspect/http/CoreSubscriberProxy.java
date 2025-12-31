package org.usf.inspect.http;

import static org.usf.inspect.core.SessionContextManager.aroundRunnable;

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
		aroundRunnable(ctx, ()-> sub.onSubscribe(s));
	}
	
	@Override
	public void onNext(T t) {
		aroundRunnable(ctx, ()-> sub.onNext(t));
	}

	@Override
	public void onError(Throwable t) {
		aroundRunnable(ctx, ()-> sub.onError(t));
	}

	@Override
	public void onComplete() {
		aroundRunnable(ctx, sub::onComplete);
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