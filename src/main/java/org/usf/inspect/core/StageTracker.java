package org.usf.inspect.core;

import static java.time.Instant.now;
import static org.usf.inspect.core.Helper.log;

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
public final class StageTracker {

	public static <E extends Throwable> void exec(SafeRunnable<E> fn, StageConsumer<? super Void> cons) throws E {
		call(fn, cons);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, StageConsumer<? super T> cons) throws E {
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
				cons.accept(s, e, o, t);
			}
			catch (Exception ex) {// do not throw exception
				log.warn("cannot collect stage metrics, {}", ex.getMessage());
			}
		}
	}
	
	@FunctionalInterface
	public static interface StageConsumer<T> {
		
		void accept(Instant start, Instant end, T o, Throwable t) throws Exception;
	}
}
