package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.aroundRunnable;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	final ExecutorService es;
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			var ctx = ses.createContext();
			return new FutureWrapper<>(es.submit(ctx.aroundCallable(task)), ctx::release);
		}
		return es.submit(task);
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			var ctx = ses.createContext();
			return new FutureWrapper<>(es.submit(ctx.aroundRunnable(task)), ctx::release);
		}
		return es.submit(task);
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			var ctx = ses.createContext();
			return new FutureWrapper<>(es.submit(ctx.aroundRunnable(task), result), ctx::release);
		}
		return es.submit(task, result);
	}
	
	@Override
	public void execute(Runnable command) {
		es.execute(aroundRunnable(command));
	}

	public static ExecutorService wrap(ExecutorService es) {
		return wrap(es, null);
	}

	public static ExecutorService wrap(@NonNull ExecutorService es, String beanName) {
		if(context().getConfiguration().isEnabled()){
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
