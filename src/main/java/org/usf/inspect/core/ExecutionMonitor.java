package org.usf.inspect.core;

import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;

import org.slf4j.Logger;
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
	
	private static final Logger log = getLogger(ExecutionMonitor.class);
	
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionMonitorListener<? super Void> listener) throws E {
		call(fn, listener);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionMonitorListener<? super T> listener) throws E {
		T o = null;
		Throwable t = null;
		var s = now();
		try {
			return (o = fn.call());
		}
		catch(Throwable e) { //also error
			t  = e;
			throw e;
		}
		finally {
			var e = now();
			try {
				listener.handle(s, e, o, t);
			}
			catch (Throwable ex) {// do not throw exception
			    log.warn("failed to notify listener after callable execution: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
			}
		}
	}

	@FunctionalInterface
	public static interface ExecutionMonitorListener<T> {
		
		void handle(Instant start, Instant end, T o, Throwable t) throws Exception;
	}
}
