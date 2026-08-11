package org.usf.inspect.core;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.SessionContextManager.aroundCallable;
import static org.usf.inspect.core.SessionContextManager.aroundRunnable;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * ExecutorService wrapper that propagates session context.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	final ExecutorService es;
	
	/**
	 * Submits a callable after wrapping it with the current session context.
	 *
	 * @param <T> the callable result type
	 * @param task the callable to submit
	 * @return a future representing the pending result
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return es.submit(aroundCallable(task));
	}
	
	/**
	 * Submits a runnable after wrapping it with the current session context.
	 *
	 * @param task the runnable to submit
	 * @return a future representing the pending task
	 */
	@Override
	public Future<?> submit(Runnable task) {
		return es.submit(aroundRunnable(task));
	}
	
	/**
	 * Submits a runnable with a predefined result after wrapping it with the current session context.
	 *
	 * @param <T> the result type
	 * @param task the runnable to submit
	 * @param result the result to return upon successful completion
	 * @return a future representing the pending result
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return es.submit(aroundRunnable(task), result);
	}
	
	/**
	 * Executes a runnable after wrapping it with the current session context.
	 *
	 * @param task the runnable to execute
	 */
	@Override
	public void execute(Runnable task) {
		es.execute(aroundRunnable(task));
	}

	/**
	 * Wraps the given executor service when tracing is enabled.
	 *
	 * @param es the executor service to wrap
	 * @return the wrapped executor service, or the original instance when wrapping is unnecessary
	 */
	public static ExecutorService wrap(ExecutorService es) {
		return wrap(es, null);
	}

	/**
	 * Wraps the given executor service when tracing is enabled.
	 *
	 * @param es the executor service to wrap
	 * @param beanName the bean name used for logging
	 * @return the wrapped executor service, or the original instance when wrapping is unnecessary
	 */
	public static ExecutorService wrap(@NonNull ExecutorService es, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			if(es.getClass() != ExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "executorService"), es.getClass());
				return new ExecutorServiceWrapper(es);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
