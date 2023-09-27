package org.usf.traceapi.core;

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.location;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceableExecutorService implements ExecutorService {
	
	static TraceSender sender;
	
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
			var st = location();
			session.lock(); // important sync lock
			es.execute(()->{
				try {
					aroundRunnable(command, session, st);
				}
				finally {
					session.unlock();
				}
			});
		}
	}
	
    private static void aroundRunnable(Runnable command, Session session, Optional<StackTraceElement> ost) {
		log.debug("stage : {} <= {}", session.getId(), command);
    	if(localTrace.get() != session) { //thread local cache
    		localTrace.set(session);
    	}
		var rs = new RunnableStage();
    	var beg = currentTimeMillis();
    	try {
    		command.run();
    	}
    	catch (Exception e) {
    		rs.setException(fromException(e));
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
	    		rs.setStart(ofEpochMilli(beg));
	    		rs.setEnd(ofEpochMilli(fin));
    			rs.setThreadName(threadName());
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
        
	public static TraceableExecutorService wrap(ExecutorService es) {
		return new TraceableExecutorService(es);
	}
	
	static void initialize(TraceSender sender) {
		TraceableExecutorService.sender = sender;
	}
	
}
