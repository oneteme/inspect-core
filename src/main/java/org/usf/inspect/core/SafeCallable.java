package org.usf.inspect.core;

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

	@FunctionalInterface
	public interface SafeConsumer<T> { //Metrics Tracker 
		
		void accept(T obj) throws Exception;
	}
	

	@FunctionalInterface
	public interface SafeSupplier<T> { //Metrics Tracker 
		
		T get() throws Exception;
	}
	

	@FunctionalInterface
	public interface SafeBiConsumer<T,O> { //Metrics Tracker 
		
		void accept(T trc, O obj) throws Exception;
	}
}