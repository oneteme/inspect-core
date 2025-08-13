package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	private final ExecutorService es;
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return aroundCallable(task, es::submit);
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		return aroundRunnable(task, es::submit);
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return aroundRunnable(task, c-> es.submit(c, result));
	}
	
	@Override
	public void execute(Runnable command) {
		aroundRunnable(command, c-> {es.execute(c); return null;});
	}
	
    private static <T> T aroundRunnable(Runnable command, Function<Runnable, T> fn) {
    	var session = requireCurrentSession();
		if(nonNull(session)) {
			session.lock(); //important! sync lock !timeout 
			try {
				return fn.apply(()->{
					session.updateContext();
			    	try {
				    	command.run();
			    	}
			    	finally {// session cleanup is guaranteed even if the task is cancelled / interrupted.
						session.unlock();
						session.releaseContext();
			    	}
				});
			}
			catch (Exception e) { 
				session.unlock();
				throw e;
			}
		}
		return fn.apply(command);
    }

    private static <T,V> V aroundCallable(Callable<T> command, Function<Callable<T>, V> fn) {
    	var session = requireCurrentSession();
		if(nonNull(session)) {
			session.lock(); //important! sync lock
			try {
				return fn.apply(()->{
					session.updateContext();
			    	try {
			    		return command.call();
			    	}
			    	finally {// session cleanup is guaranteed even if the task is cancelled/interrupted.
						session.unlock();
						session.releaseContext();
			    	}
				});
			}
			catch (Exception e) {
				session.unlock();
				throw e;
			}
		}
		return fn.apply(command);
    }
    
	public static ExecutorService wrap(@NonNull ExecutorService es) {
		if(context().getConfiguration().isEnabled()){
			logWrappingBean("executorService", es.getClass());
			return new ExecutorServiceWrapper(es);
		}
		return es;
	}
}
