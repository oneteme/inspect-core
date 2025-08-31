package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;

import java.time.Instant;
import java.util.concurrent.Callable;

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
	
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionHandler<? super Void> listener) throws E {
		call(fn, listener);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionHandler<? super T> listener) throws E {
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
			trigger(listener, s, now(), o, t);
		}
	}
	
	public static <T> void trigger(ExecutionHandler<T> handler, Instant start, Instant end, T obj, Throwable thrw) {
		try {
			var trace = handler.handle(start, end, obj, thrw);
			if(nonNull(trace)) {
				trace.emit();
			}
		}
		catch (Throwable ex) {// do not throw exception
			reporter().action("EventTrace.handle").cause(ex).emit();
		}
	}
	
	public static void call(Callable<EventTrace> call)  {
		try {
			var trace = call.call();
			if(nonNull(trace)) {
				trace.emit();
			}
		}
		catch (Throwable ex) {// do not throw exception
			reporter().action("EventTrace.call").cause(ex).emit();
		}
	}
	
	@FunctionalInterface
	public static interface ExecutionHandler<T> {
		
		EventTrace handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception;
	}
}
