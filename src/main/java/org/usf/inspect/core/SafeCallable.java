package org.usf.inspect.core;

/**
 * A callable that may throw a checked exception, serving as the base for runnable and consumer variants used in the tracing framework.
 *
 * @param <T> the return type of the callable
 * @param <E> the checked exception type that may be thrown
 * @author u$f
 *
 */
@FunctionalInterface
public interface SafeCallable<T, E extends Throwable> { //Metrics Tracker
	
	/**
	 * Invokes the callable.
	 *
	 * @return the computed result
	 * @throws E if the callable encounters an error
	 */
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
	public interface SafeBiConsumer<T, U> {

	    void accept(T t, U u) throws Exception;
	}
}