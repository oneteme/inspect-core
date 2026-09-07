package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

/**
 * Contract for tracers operating on a dual-event telemetry model.
 * <p>
 * This pattern orchestrates trace dispatching in two distinct phases:
 * <ul>
 *   <li><b>An initial event (Signal):</b> emitted at the start of an operation to immediately 
 *       notify the initiation of execution and propagate context.</li>
 *   <li><b>A final event (Update):</b> emitted upon completion to enrich the trace with actual duration, 
 *       final execution status, and captured exceptions.</li>
 * </ul>
 * 
 * It also provides shared mechanics for error status resolution and active trace lifecycle validation.
 *
 * @author u$f
 * 
 * @since 1.1
 */
public interface DualEventTracer {
	
	static final short UNKNOWN             	= -1; // Unknown / Unspecified status

	static final short CONN_ERROR   			= 0; // Generic I/O or Transport failure
    static final short CONN_UNKNOWN_HOST  	= 1; // Host / DNS resolution failed
    static final short CONN_REFUSED       	= 2; // Connection refused or unreachable
    static final short CONN_INTERRUPTED   	= 3; // Connection interrupted or cancelled
    static final short CONN_TIMEOUT       	= 4; // Connection establishment timeout
    static final short CONN_SSL_ERROR      	= 5; // SSL/TLS handshake or certificate failure

    static final short SUCCESS            	= 200; // Success / OK

    static final short CLIENT_ERROR       	= 400; // Generic client-side error
    static final short CLIENT_UNAUTHORIZED	= 401; // Authentication or permission failure
    static final short CLIENT_TIMEOUT    		= 408; // Client-side operation timeout
    static final short CLIENT_CONFLICT     	= 409; // Duplicate key / Constraint violation

    static final short SERVER_ERROR      		= 500; // Generic server/remote error
    static final short SERVER_TIMEOUT     	= 504; // Server/Gateway response timeout
    
    //TD dev exception => status
    
	TraceUpdate getUpdate();

	default short resolveStatus(Throwable t){
		return SERVER_ERROR; //default status 
	}
	
	default Throwable mapException(Throwable t) {
		return getUpdate() instanceof AbstractSessionUpdate ? t : rootCauseException(t);
	}
	
	default ExceptionTrace exceptionTrace(Throwable t, long offset) {
		var upd = getUpdate();
		var exp = upd instanceof AbstractSessionUpdate ? fromException(t) : fromException(t, 0, 0);
		exp.setOffset(offset);
		exp.setTraceId(upd.getId());
		return exp;
	}
	
    default boolean assertActiveTraceUpdate(String action) {
		var upd = getUpdate();
		if(nonNull(upd)) {
			if(isNull(upd.getEnd())) {
				return true;
			}
			else {
				hub().reportMessage(true, action, "trace update is already completed");
			}
		}
		else {
			hub().reportMessage(true, action, "trace update is null");
		}
    	return false;
    }
    
    public static boolean assertActiveTracer(DualEventTracer tracer, String action) {
    	if(nonNull(tracer)) {
    		return tracer.assertActiveTraceUpdate(action);
    	}
		hub().reportMessage(true, action, "tracer is null");
    	return false;
    }
    
	static Throwable rootCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return t;
		}
		return t;
	}

    interface StageBuilder<R> {
		
		AbstractStage newStage(Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
