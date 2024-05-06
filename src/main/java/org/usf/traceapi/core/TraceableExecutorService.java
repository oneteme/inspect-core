package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.warnNoSession;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceableExecutorService implements ExecutorService {
	
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
    	var session = localTrace.get();
		if(isNull(session)) {
			warnNoSession();
			return fn.apply(command);
		}
		session.lock(); //important! sync lock
		try {
			var ost = stackTraceElement(); //important! on parent thread
			return fn.apply(()->{
				log.trace("stage : {} <= {}", session.getId(), command);
		    	if(localTrace.get() != session) {
		    		localTrace.set(session); //thread already exists
		    	}
				Throwable ex = null;
		    	var beg = now();
		    	try {
		    		command.run();
		    	}
		    	catch (Exception e) {
		    		ex =  e;
		    		throw e;
		    	}
		    	finally {
		    		var fin = now();
		    		try {
		    	    	var rs = new RunnableStage();
			    		rs.setStart(beg);
			    		rs.setEnd(fin);
		    			rs.setThreadName(threadName());
		    			rs.setException(mainCauseException(ex));
			    		ost.ifPresent(st->{
				    		rs.setName(st.getMethodName());
				    		rs.setLocation(st.getClassName());
			    		});
		    			session.append(rs);
		    		}
		    		catch(Exception e) {
						log.warn("error while tracing : " + command, e);
						//do not throw exception
		    		}
					session.unlock();
					localTrace.remove(); 
		    	}
			});
		}
		catch (Exception e) {  //@see Executor::execute
			session.unlock();
			throw e;
		}
    }

    private static <T,V> V aroundCallable(Callable<T> command, Function<Callable<T>, V> fn) {
    	var session = localTrace.get();
		if(isNull(session)) {
			warnNoSession();
			return fn.apply(command);
		}
		session.lock(); //important! sync lock
		try {
			var ost = stackTraceElement(); //important! on parent thread
			return fn.apply(()->{
				log.trace("stage : {} <= {}", session.getId(), command);
		    	if(localTrace.get() != session) {
		    		localTrace.set(session); //thread already exists
		    	}
				Throwable ex = null;
		    	var beg = now();
		    	try {
		    		return command.call();
		    	}
		    	catch (Exception e) {
		    		ex =  e;
		    		throw e;
		    	}
		    	finally {
		    		var fin = now();
		    		try {
		    	    	var rs = new RunnableStage();
			    		rs.setStart(beg);
			    		rs.setEnd(fin);
		    			rs.setThreadName(threadName());
		    			rs.setException(mainCauseException(ex));
			    		ost.ifPresent(st->{
				    		rs.setName(st.getMethodName());
				    		rs.setLocation(st.getClassName());
			    		});
		    			session.append(rs);
		    		}
		    		catch(Exception e) {
						log.warn("error while tracing : " + command, e);
						//do not throw exception
		    		}
					session.unlock();
					localTrace.remove(); 
		    	}
			});
		}
		catch (Exception e) {  //@see Executor::execute
			session.unlock();
			throw e;
		}
    }
        
	public static TraceableExecutorService wrap(@NonNull ExecutorService es) {
		return new TraceableExecutorService(es);
	}
}
