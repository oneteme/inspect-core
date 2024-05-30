package org.usf.traceapi.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SafeSupplier<T, E extends Throwable> { //Metrics Tracker
	
	T get() throws E;
	
	@FunctionalInterface
	interface SafeRunnable<E extends Throwable> extends SafeSupplier<Void, E> {
		
		void run() throws E;
		
		default Void get() throws E {
			this.run();
			return null;
		}
	}
	
	@FunctionalInterface
	interface MetricsConsumer<T> {
		
		void accept(Instant start, Instant end, T o, Throwable t) throws Exception;
		
		default MetricsConsumer<T> thenAccept(MetricsConsumer<? super T> other){
			return (s,e,o,t)-> {accept(s, e, o, t); other.accept(s, e, o, t); };
		}
	}
}