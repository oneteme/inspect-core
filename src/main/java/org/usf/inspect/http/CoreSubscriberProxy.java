package org.usf.inspect.http;

import static org.usf.inspect.core.SessionContextManager.runWithContext;

import org.reactivestreams.Subscription;
import org.usf.inspect.core.AbstractSessionUpdate;

import lombok.RequiredArgsConstructor;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * Delegates Reactor subscriber callbacks while restoring the inspection session context.
 */
@RequiredArgsConstructor
public final class CoreSubscriberProxy<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<T> sub;
	private final AbstractSessionUpdate ctx;

	/**
	 * Delegates subscription initialization within the tracked session context.
	 *
	 * @param s the subscription to pass to the wrapped subscriber
	 */
	@Override
	public void onSubscribe(Subscription s) {
		runWithContext(ctx, ()-> sub.onSubscribe(s), ctx::threadCountUp);
	}
	
	/**
	 * Delegates the next emitted item within the tracked session context.
	 *
	 * @param t the emitted item
	 */
	@Override
	public void onNext(T t) {
		runWithContext(ctx, ()-> sub.onNext(t), null);
	}

	/**
	 * Delegates an error signal within the tracked session context.
	 *
	 * @param t the emitted error
	 */
	@Override
	public void onError(Throwable t) {
		runWithContext(ctx, ()-> sub.onError(t), ctx::threadCountDown);
	}

	/**
	 * Delegates completion within the tracked session context.
	 */
	@Override
	public void onComplete() {
		runWithContext(ctx, sub::onComplete, ctx::threadCountDown);
	}

	/**
	 * Returns the current Reactor context of the wrapped subscriber.
	 *
	 * @return the wrapped subscriber context
	 */
	@Override
	public Context currentContext() {
		return sub.currentContext();
	}
	
	/**
	 * Compares this proxy using the wrapped subscriber implementation.
	 *
	 * @param obj the object to compare with
	 * @return {@code true} when the wrapped subscriber considers the objects equal
	 */
	@Override
	public boolean equals(Object obj) {
		return sub.equals(obj);
	}
	
	/**
	 * Returns the hash code of the wrapped subscriber.
	 *
	 * @return the wrapped subscriber hash code
	 */
	@Override
	public int hashCode() {
		return sub.hashCode();
	}
	
	/**
	 * Returns the string representation of the wrapped subscriber.
	 *
	 * @return the wrapped subscriber string representation
	 */
	@Override
	public String toString() {
		return sub.toString();
	}
}