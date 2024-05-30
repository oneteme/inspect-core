package org.usf.traceapi.core;

import static java.time.Instant.now;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.log;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SafeSupplier<T, E extends Throwable> {
	
	T get() throws E;
	
	public static <E extends Throwable> void call(SafeCallable<E> sqlSupp, MetricsConsumer<Void> cons) throws E {
		supply(sqlSupp, cons);
	}
	
	public static <E extends Throwable> void call(Supplier<Instant> startSupp, SafeCallable<E> sqlSupp, MetricsConsumer<Void> cons) throws E {
		supply(startSupp, sqlSupp, cons);
	}
	
	public static <T, E extends Throwable> T supply(SafeSupplier<T,E> sqlSupp, MetricsConsumer<T> cons) throws E {
		return supply(Instant::now, sqlSupp, cons);
	}

	public static <T, E extends Throwable> T supply(Supplier<Instant> startSupp, SafeSupplier<T,E> sqlSupp, MetricsConsumer<T> cons) throws E {
		Throwable ex = null;
		T res = null;
		var beg = startSupp.get();
		try {
			return res = sqlSupp.get();
		}
		catch(Throwable e) { //also error
			ex  = e;
			throw e;
		}
		finally {
			var fin = now();
			try {
				cons.accept(beg, fin, res, mainCauseException(ex));
			}
			catch (Exception e) {
				//do not throw exception
				log.warn("cannot execute {}, because={}", cons, e.getMessage());
			}
		}
	}

	@FunctionalInterface
	interface SafeCallable<E extends Throwable> extends SafeSupplier<Void, E> {
		
		void call() throws E;
		
		default Void get() throws E {
			this.call();
			return null;
		}
	}
	
	@FunctionalInterface
	interface MetricsConsumer<T> {
		
		void accept(Instant start, Instant end, T o, ExceptionInfo ex);
		
		default MetricsConsumer<T> thenAccept(MetricsConsumer<? super T> other){
			return (s,e,o,ex)-> {accept(s, e, o, ex); other.accept(s, e, o, ex); };
		}
		
	}
}