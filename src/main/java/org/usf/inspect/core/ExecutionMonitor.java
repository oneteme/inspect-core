package org.usf.inspect.core;

import static java.time.Instant.now;
import static org.usf.inspect.core.ErrorReporter.stackReporter;

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
public final class ExecutionMonitor {
	
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionHandler<? super Void> handler) throws E {
		call(fn, handler);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionHandler<? super T> handler) throws E {
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
			notifyHandler(handler, s, now(), o, t);
		}
	}
	
	public static <T> void notifyHandler(ExecutionHandler<T> handler, Instant start, Instant end, T obj, Throwable thrw) {
		try {
			handler.handle(start, end, obj, thrw);
		}
		catch (Throwable ex) {// do not throw exception
			stackReporter().action("ExecutionMonitor.handle").cause(ex).emit();
		}
	}
	
	public static void runSafely(Runnable call)  {
		try {
			call.run();
		}
		catch (Throwable ex) {// do not throw exception
			stackReporter().action("ExecutionMonitor.call").cause(ex).emit();
		}
	}
	
	@FunctionalInterface
	public static interface ExecutionHandler<T> {
		
		void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception;
	}
}
