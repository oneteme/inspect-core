package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.ContextPropagators.wrapCallable;
import static org.usf.inspect.core.ContextPropagators.wrapRunnable;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	final ExecutorService es;
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return es.submit(wrapCallable(task));
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		return es.submit(wrapRunnable(task));
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return es.submit(wrapRunnable(task), result);
	}
	
	@Override
	public void execute(Runnable task) {
		es.execute(wrapRunnable(task));
	}

	public static ExecutorService wrap(ExecutorService es) {
		return wrap(es, null);
	}
	
	public static ExecutorService wrap(ExecutorService es, String beanName) {
		if(nonNull(es) && getConfiguration().isEnabled()){
			if(es.getClass() != ExecutorServiceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "executorService"), es.getClass());
				return new ExecutorServiceWrapper(es);
			}
			else {
				REPORTER.warn("{}: {} is already wrapped", beanName, es);
			}
		}
		return es;
	}
}
