package org.usf.traceapi.core;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceableExecutorService implements ExecutorService {
	
	@Delegate
	private final ExecutorService es;
	
	@Override
	public void execute(Runnable command) {
		var session = localTrace.get();
		if(isNull(session)) {
			log.warn("no session");
			es.execute(command);
		}
		else {
			session.lock(); //important! sync lock
			var st = stackTraceElement(); //important! on parent thread
			try {
				es.execute(()->{
					try {
						aroundRunnable(command, session, st);
					}
					finally {
						session.unlock();
					}
				});
			}
			catch (Exception e) {  //@see Executor::execute
				session.unlock();
				throw e;
			}
		}
	}
	
    private static void aroundRunnable(Runnable command, Session session, Optional<StackTraceElement> ost) {
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
			localTrace.remove();
    	}
    }
        
	public static TraceableExecutorService wrap(@NonNull ExecutorService es) {
		return new TraceableExecutorService(es);
	}	
}
