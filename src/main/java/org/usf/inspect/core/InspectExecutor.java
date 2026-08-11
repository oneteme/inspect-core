package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.SafeCallable.SafeRunnable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides helper methods for executing callbacks while reporting execution details.
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InspectExecutor {

	/**
	 * Executes the given runnable and notifies the listener with the execution outcome.
	 * 
	 * @param fn the runnable to execute
	 * @param handler the listener to notify after execution completes
	 * @param <E> the exception type that may be thrown by the runnable
	 * @throws E if the runnable throws an exception of type {@code E}
	 */
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionListener<? super Void> handler) throws E {
		call(fn, handler);
	}

	/**
	 * Executes the given callable and notifies the listener with the execution outcome.
	 * 
	 * @param fn the callable to execute
	 * @param handler the listener to notify after execution completes
	 * @param <T> the result type returned by the callable
	 * @param <E> the exception type that may be thrown by the callable
	 * @return the value returned by the callable
	 * @throws E if the callable throws an exception of type {@code E}
	 */
	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionListener<? super T> handler) throws E {
		T o = null;
		Throwable t = null;
		var s = systemUTC().instant();
		try {
			return (o = fn.call());
		}
		catch(Throwable e) { //also error
			t = e;
			throw e;
		}
		finally {
			if(nonNull(handler)) {
				handler.safeHandle(s, systemUTC().instant(), o, t);
			}
		}
	}
	
	/**
	 * Listens to execution events emitted by {@link InspectExecutor}.
	 *
	 * @param <T> the result type observed by the listener
	 */
	@FunctionalInterface
	public static interface ExecutionListener<T> {
		
		/**
		 * Handles an execution result with its timing and failure details.
		 * 
		 * @param start the execution start instant
		 * @param end the execution end instant
		 * @param obj the execution result, or {@code null} when unavailable
		 * @param thrw the exception raised during execution, or {@code null} when none occurred
		 * @throws Exception if handling fails
		 */
		void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception;

		/**
		 * Handles an execution result while suppressing any listener failure.
		 * 
		 * @param start the execution start instant
		 * @param end the execution end instant
		 * @param res the execution result, or {@code null} when unavailable
		 * @param thrw the exception raised during execution, or {@code null} when none occurred
		 */
		default void safeHandle(Instant start, Instant end, T res, Throwable thrw) {
			try {
				handle(start, end, res, thrw);
			}
			catch (Throwable ex) {// do not throw exception
				hub().reportError(true, "ExecutionMonitor.safeHandle", ex);
			}
		}
		
		/**
		 * Chains this listener with another listener executed afterward.
		 * 
		 * @param next the listener to invoke after this listener
		 * @return a composed listener that invokes both listeners in sequence
		 */
		default ExecutionListener<T> thenHandle(ExecutionListener<? super T> next) {
			return (s,e,o,t)-> {
				handle(s,e,o,t);
				if(nonNull(next)) {
					next.handle(s,e,o,t);
				}
				else {
					hub().reportError(true, "ExecutionMonitor.thenHandle", new NullPointerException("next is null"));
				}
			};
		}
	}
}
