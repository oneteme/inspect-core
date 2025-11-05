package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;
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
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutorServiceWrapper implements ExecutorService {
	
	@Delegate
	private final ExecutorService es;
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return es.submit(aroundCallable(task));
	}
	
	@Override
	public Future<?> submit(Runnable task) {
		 return es.submit(aroundRunnable(task));
	}
	
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		 return es.submit(aroundRunnable(task), result);
	}
	
	@Override
	public void execute(Runnable command) {
		es.execute(aroundRunnable(command));
	}
	
    public static Runnable aroundRunnable(Runnable command) {
    	var ses = requireCurrentSession();
		return nonNull(ses) ? ()->{
			ses.updateContext();
	    	try {
		    	command.run();
	    	}
	    	finally {// session cleanup is guaranteed even if the task is cancelled / interrupted.
				ses.releaseContext();
	    	}
		} : command;
    }

    public static <T> Callable<T> aroundCallable(Callable<T> command) {
    	var session = requireCurrentSession();
		return nonNull(session) ? ()->{
			session.updateContext();
	    	try {
	    		return command.call();
	    	}
	    	finally {// session cleanup is guaranteed even if the task is cancelled/interrupted.
				session.releaseContext();
	    	}
		} : command;
    }

	public static ExecutorService wrap(@NonNull ExecutorService es) {
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
