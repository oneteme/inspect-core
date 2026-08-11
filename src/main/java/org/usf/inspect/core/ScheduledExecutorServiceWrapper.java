package org.usf.inspect.core;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.SessionContextManager.aroundCallable;
import static org.usf.inspect.core.SessionContextManager.aroundRunnable;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * ScheduledExecutorService wrapper that propagates session context to scheduled tasks.
 *
 * @author u$f
 *
 */
@Slf4j
public class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {

	 ScheduledExecutorServiceWrapper(ScheduledExecutorService es) {
		super(es);
	}

	/**
	 * Schedules a callable to execute after the given delay, wrapped with the current session context.
	 *
	 * @param <V> the callable result type
	 * @param task the callable to schedule
	 * @param delay the time to delay the execution
	 * @param unit the time unit of the delay
	 * @return a scheduled future representing the pending result
	 */
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		return se().schedule(aroundCallable(task), delay, unit);
	}

	/**
	 * Schedules a runnable to execute after the given delay, wrapped with the current session context.
	 *
	 * @param task the runnable to schedule
	 * @param delay the time to delay the execution
	 * @param unit the time unit of the delay
	 * @return a scheduled future representing the pending task
	 */
	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		return se().schedule(aroundRunnable(task), delay, unit);
	}

	/**
	 * Schedules a runnable to execute repeatedly at a fixed rate, wrapped with the current session context.
	 *
	 * @param task the runnable to schedule
	 * @param initialDelay the delay before the first execution
	 * @param period the period between successive executions
	 * @param unit the time unit for the delay and period
	 * @return a scheduled future representing the pending task
	 */
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		return se().scheduleAtFixedRate(aroundRunnable(task), initialDelay, period, unit);
	}

	/**
	 * Schedules a runnable to execute repeatedly with a fixed delay, wrapped with the current session context.
	 *
	 * @param task the runnable to schedule
	 * @param initialDelay the delay before the first execution
	 * @param delay the delay between the end of one execution and the start of the next
	 * @param unit the time unit for the delays
	 * @return a scheduled future representing the pending task
	 */
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		return se().scheduleWithFixedDelay(aroundRunnable(task), initialDelay, delay, unit);
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}

	/**
	 * Wraps the given scheduled executor service when tracing is enabled.
	 *
	 * @param es the scheduled executor service to wrap
	 * @return the wrapped service, or the original instance when wrapping is unnecessary
	 */
	public static ScheduledExecutorService wrap(ScheduledExecutorService es) {
		return wrap(es, null);
	}
	
	/**
	 * Wraps the given scheduled executor service when tracing is enabled.
	 *
	 * @param es the scheduled executor service to wrap
	 * @param beanName the bean name used for logging
	 * @return the wrapped service, or the original instance when wrapping is unnecessary
	 */
	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			if(es.getClass() != ScheduledExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "executorService"), es.getClass());
				return new ScheduledExecutorServiceWrapper(es);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
