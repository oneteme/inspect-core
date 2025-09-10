package org.usf.inspect.http;

import org.reactivestreams.Subscription;
import org.usf.inspect.core.AbstractSession;

import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * 
 * @author u$f
 *
 */
public final class CoreSubscriberProxy<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<T> sub;
	private final AbstractSession session;

	public CoreSubscriberProxy(CoreSubscriber<T> sub, AbstractSession session) { //lock
		this.sub = sub;
		this.session = session;
	}

	@Override
	public void onNext(T t) {
		session.updateContext();
		sub.onNext(t);
	}

	@Override
	public void onError(Throwable t) { //unlock
		session.updateContext();
		sub.onError(t);
	}

	@Override
	public void onComplete() { //unlock
		session.updateContext();
		sub.onComplete();				
	}

	@Override
	public void onSubscribe(Subscription s) {
		session.updateContext();
		sub.onSubscribe(s);
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