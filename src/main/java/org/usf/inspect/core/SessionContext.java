package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.usf.inspect.core.SessionManager.currentSession;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionContext {
	
	private static final ScheduledExecutorService ES = newSingleThreadScheduledExecutor();

	private final Future<?> future;
	private final AbstractSession session;
	
	Runnable aroundRunnable(Runnable cmd) {
		return ()-> call(()-> {cmd.run(); return null;});
	}
	
	<T> Callable<T> aroundCallable(Callable<T> cmd) {
		return ()-> call(cmd::call);
	}
	
	<T> Supplier<T> aroundSupplier(Supplier<T> cmd) {
		return ()-> call(cmd::get);
	}
	
	<T, E extends Exception> T call(SafeCallable<T, E> call) throws E {
		release(); 
		session.countUp();
		var prv = currentSession();
		if(prv != session) {
			session.updateContext();
		}
		try {
			return call.call();
		}
		finally {
			session.countDown();
			if(prv != session) {
				session.releaseContext();
				if(nonNull(prv)) {
					prv.updateContext();
				}
			}
		}	
	}
	
	public boolean release() {
		if(future.isDone() || future.isCancelled()) {
			return true;
		}
		if(future.cancel(true)) {
			session.countDown();
			return true;
		}
		return false;
	}

	public static SessionContext context(long delay, AbstractSession session) {
		session.countUp();
		return new SessionContext(ES.schedule(session::countDown, delay, MILLISECONDS), session);
	}
}
