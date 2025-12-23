package org.usf.inspect.core;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.TraceDispatcherHub.hub;
import static org.usf.inspect.core.SessionContextManager.aroundCallable;
import static org.usf.inspect.core.SessionContextManager.aroundRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {

	 ScheduledExecutorServiceWrapper(ScheduledExecutorService es) {
		super(es);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
		return se().schedule(aroundCallable(task), delay, unit);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		return se().schedule(aroundRunnable(task), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
		return se().scheduleAtFixedRate(aroundRunnable(task), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
		return se().scheduleWithFixedDelay(aroundRunnable(task), initialDelay, delay, unit);
	}
	
	ScheduledExecutorService se() {
		return (ScheduledExecutorService) es;
	}

	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es) {
		return wrap(es, null);
	}
	
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
