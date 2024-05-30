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
	
	public static <E extends Throwable> void call(SafeRunnable<E> sqlSupp, MetricsConsumer<Void> cons) throws E {
		supply(sqlSupp, cons);
	}

	public static <E extends Throwable> void call(Supplier<Instant> startSupp, SafeRunnable<E> sqlSupp, MetricsConsumer<Void> cons) throws E {
		supply(startSupp, sqlSupp, cons);
	}

	public static <T, E extends Throwable> T supply(SafeSupplier<T,E> sqlSupp, MetricsConsumer<T> cons) throws E {
		return supply(Instant::now, sqlSupp, cons);
	}

	public static <T, E extends Throwable> T supply(Supplier<Instant> startSupp, SafeSupplier<T,E> sqlSupp, MetricsConsumer<T> cons) throws E {
		T res = null;
		Throwable ex = null;
		var beg = startSupp.get();
		try {
			return (res = sqlSupp.get());
		}
		catch(Throwable t) { //also error
			ex  = t;
			throw t;
		}
		finally {
			var fin = now();
			try {
				cons.accept(beg, fin, res, ex);
			}
			catch (Exception e) {
				//do not throw exception
				log.warn("cannot execute {}, because={}", cons, e.getMessage());
			}
		}
	}

}
