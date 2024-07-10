package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SafeCallable<T, E extends Throwable> { //Metrics Tracker
	
	T call() throws E;
	
	@FunctionalInterface
	interface SafeRunnable<E extends Throwable> extends SafeCallable<Void, E> {
		
		void run() throws E;
		
		@Override
		default Void call() throws E {
			this.run();
			return null;
		}
	}
}