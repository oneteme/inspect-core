package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {

	 ScheduledExecutorServiceWrapper(ScheduledExecutorService es) {
		super(es);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			var ctx = ses.createContext(delay, unit);
			return new ScheduledFutureWrapper<>(get().schedule(ctx.aroundRunnable(command), delay, unit), ctx::release);
		}
		return get().schedule(command, delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		var ses = requireCurrentSession();
		if(nonNull(ses)) {
			var ctx = ses.createContext(delay, unit);
			return new ScheduledFutureWrapper<>(get().schedule(ctx.aroundCallable(callable), delay, unit), ctx::release);
		}
		return get().schedule(callable, delay, unit);
	}

	@Delegate
	ScheduledExecutorService get() {
		return (ScheduledExecutorService) es;
	}

	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es) {
		return wrap(es, null);
	}
	
	public static ScheduledExecutorService wrap(@NonNull ScheduledExecutorService es, String beanName) {
		if(context().getConfiguration().isEnabled()){
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
