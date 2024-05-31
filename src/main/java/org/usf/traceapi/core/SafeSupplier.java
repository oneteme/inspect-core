package org.usf.traceapi.core;

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
	
}