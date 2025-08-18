package org.usf.inspect.rest;

import org.reactivestreams.Subscription;
import org.usf.inspect.core.Session;

import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

public final class CoreSubscriberProxy<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<T> sub;
	private final Session session;

	public CoreSubscriberProxy(CoreSubscriber<T> sub, Session session) { //lock
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
	public String toString() {
		return sub.toString();
	}
}