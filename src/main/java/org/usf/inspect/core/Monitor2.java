package org.usf.inspect.core;

import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;

public interface Monitor2 {
	
    static final int CONN_ERROR   			= 0; // Generic I/O or Transport failure
    static final int CONN_UNKNOWN_HOST  	= 1; // Host / DNS resolution failed
    static final int CONN_REFUSED       	= 2; // Connection refused or unreachable
    static final int CONN_INTERRUPTED   	= 3; // Connection interrupted or cancelled
    static final int CONN_TIMEOUT       	= 4; // Connection establishment timeout
    static final int CONN_SSL_ERROR      	= 5; // SSL/TLS handshake or certificate failure

    static final int SUCCESS            	= 200; // Success / OK

    static final int CLIENT_ERROR       	= 400; // Generic client-side error
    static final int CLIENT_UNAUTHORIZED	= 401; // Authentication or permission failure
    static final int CLIENT_TIMEOUT    		= 408; // Client-side operation timeout
    static final int CLIENT_CONFLICT     	= 409; // Duplicate key / Constraint violation

    static final int SERVER_ERROR      		= 500; // Generic server/remote error
    static final int SERVER_TIMEOUT     	= 504; // Server/Gateway response timeout
    
    static final ExecutionListener<?> NO_UPDATE_LISTENER = (s,e,o,t)-> 
    	hub().reportMessage(true, "Monitor2.executionListener", "update is null");
    
	
	TraceSignal signal(Instant start);
	
	TraceUpdate update(TraceSignal signal);
	
	default int resolveStatus(Throwable t){
		return SERVER_ERROR;
	}
	
	default Throwable exception(Throwable t) {
		return t;
	}
	
	default ExceptionTrace exceptionTrace(Throwable t, TraceUpdate update, long offset) {
		var exp = update instanceof AbstractSessionUpdate ? fromException(t) : fromException(t, 0, 0);
		exp.setOffset(offset);
		exp.setTraceId(update.getId());
		return exp;
	}
	
	@SuppressWarnings("unchecked")
	static <R> ExecutionListener<R> noUpdateExecutionListenner(){
		return (ExecutionListener<R>) NO_UPDATE_LISTENER;
	}
}
