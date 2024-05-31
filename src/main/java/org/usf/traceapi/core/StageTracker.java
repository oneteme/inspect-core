package org.usf.traceapi.core;

import static java.time.Instant.now;
import static org.usf.traceapi.core.Helper.log;

import java.time.Instant;
import java.util.function.Supplier;

import org.usf.traceapi.core.SafeSupplier.MetricsConsumer;
import org.usf.traceapi.core.SafeSupplier.SafeRunnable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MetricsTracker {

	public static <E extends Throwable> void call(SafeRunnable<E> fn, MetricsConsumer<Void> cons) throws E {
		supply(fn, cons);
	}

	public static <E extends Throwable> void call(Supplier<Instant> startSupp, SafeRunnable<E> fn, MetricsConsumer<Void> cons) throws E {
		supply(startSupp, fn, cons);
	}

	public static <T, E extends Throwable> T supply(SafeSupplier<T,E> fn, MetricsConsumer<T> cons) throws E {
		return supply(Instant::now, fn, cons);
	}

	public static <T, E extends Throwable> T supply(Supplier<Instant> startSupp, SafeSupplier<T,E> fn, MetricsConsumer<T> cons) throws E {
		T o = null;
		Throwable t = null;
		var s = startSupp.get();
		try {
			return (o = fn.get());
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
			catch (Exception ex) {
				//do not throw exception
				log.warn("cannot collect metrics, {}", ex.getMessage());
			}
		}
	}
}
