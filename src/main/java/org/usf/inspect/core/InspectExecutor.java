package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;

import java.time.Instant;

import org.usf.inspect.core.SafeCallable.SafeRunnable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectExecutor {
	
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionListener<? super Void> handler) throws E {
		call(fn, handler);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionListener<? super T> handler) throws E {
		T o = null;
		Throwable t = null;
		var s = now();
		try {
			return (o = fn.call());
		}
		catch(Throwable e) { //also error
			t = e;
			throw e;
		}
		finally {
			if(nonNull(handler)) {
				handler.fire(s, now(), o, t);
			}
		}
	}
	
	@Deprecated(forRemoval = true)
	public static void runSafely(Runnable call)  {
		try {
			call.run();
		}
		catch (Throwable ex) {// do not throw exception
			context().reportError(true, "ExecutionMonitor.runSafely", ex);
		}
	}
	
	@FunctionalInterface
	public static interface ExecutionListener<T> {
		
		void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception;

		default void fire(Instant start, Instant end, T res, Throwable thrw) {
			try {
				handle(start, end, res, thrw);
			}
			catch (Throwable ex) {// do not throw exception
				context().reportError(true, "ExecutionMonitor.notifyHandler", ex);
			}
		}
	}
}
