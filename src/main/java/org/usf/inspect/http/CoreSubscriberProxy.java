package org.usf.inspect.http;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionPropagator.withContext;
import static org.usf.inspect.core.TraceHub.hub;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscription;
import org.usf.inspect.core.AbstractSessionUpdate;

import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.util.context.Context;

/**
 * Restores the inspect session around every Reactor signal.
 * <p>
 * The first proxy of a chain (root, resolved from the current thread) publishes the session into the Reactor
 * {@link Context} and owns the session thread counter (released once on terminal signal or {@code cancel}).
 * Upstream proxies (resolved from the {@link Context}) only restore the {@code ThreadLocal}: no counting,
 * no subscription wrapping, so operator fusion is preserved.
 *
 * @author u$f
 */
public final class CoreSubscriberProxy<T> implements CoreSubscriber<T>, Subscription {

	static final String CONTEXT_KEY = "inspect.session";
	
	private final CoreSubscriber<? super T> sub;
	private final AbstractSessionUpdate ctx;
	private final AtomicBoolean done; //root only
	private volatile Subscription s; //root only
	private volatile Context context; //root only, lazy

	private CoreSubscriberProxy(CoreSubscriber<? super T> sub, AbstractSessionUpdate ctx, boolean root) {
		this.sub = sub;
		this.ctx = ctx;
		this.done = root ? new AtomicBoolean() : null;
	}

	public static <T> CoreSubscriber<? super T> lift(Scannable scn, CoreSubscriber<? super T> sub) {
		if (sub instanceof CoreSubscriberProxy<?>) {
	        return sub;
	    }
		AbstractSessionUpdate ctx = sub.currentContext().getOrDefault(CONTEXT_KEY, null);
		if(nonNull(ctx)) {
			return new CoreSubscriberProxy<>(sub, ctx, false); //upstream of a root
		}
		ctx = activeContext();
		return nonNull(ctx) && !ctx.wasCompleted() ? new CoreSubscriberProxy<>(sub, ctx, true) : sub;
	}

	@Override
	public void onSubscribe(Subscription s) {
		if(isNull(done)) {
			try(var sp = withContext(ctx, false)) {
				sub.onSubscribe(s); //pass-through, keeps fusion
			}
		}
		else {
			this.s = s;
			try(var sp = withContext(ctx, false)) {
				ctx.threadCountUp(); //before any synchronous signal
				sub.onSubscribe(this);
			}
		}
	}

	@Override
	public void onNext(T t) {
		try(var sp = withContext(ctx, false)) {
			sub.onNext(t);
		}
	}

	@Override
	public void onError(Throwable t) {
		try(var sp = withContext(ctx, false)) {
			sub.onError(t);
		}
		finally {
			terminate();
		}
	}

	@Override
	public void onComplete() {
		try(var c = withContext(ctx, false)) {
			sub.onComplete();
		}
		finally {
			terminate();
		}
	}

	@Override
	public Context currentContext() {
		if(isNull(done)) {
			return sub.currentContext(); //already contains the session
		}
		var c = context;
		if(isNull(c)) {
			synchronized (this) {
				context = c = sub.currentContext().put(CONTEXT_KEY, ctx);
			}
		}
		return c;
	}

	@Override
	public void request(long n) {
		if(nonNull(s)) {
			s.request(n);
		}
		else {
			hub().emitReport("CoreSubscriberProxy.request", "s is null");
		}
	}

	@Override
	public void cancel() {
		if(nonNull(s)) {
			try {
				s.cancel();
			}
			finally {
				terminate();
			}
		}
		else {
			hub().emitReport("CoreSubscriberProxy.cancel", "s is null");
		}
	}

	private void terminate() {
		if(nonNull(done) && done.compareAndSet(false, true)) {
			ctx.threadCountDown();
		}
	}
}