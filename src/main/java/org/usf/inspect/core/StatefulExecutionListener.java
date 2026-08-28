package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StageBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class StatefulExecutionListener<T> {

    public static final int CONN_ERROR   		= 0; // Generic I/O or Transport failure
    public static final int CONN_UNKNOWN_HOST  	= 1; // Host / DNS resolution failed
    public static final int CONN_REFUSED       	= 2; // Connection refused or unreachable
    public static final int CONN_INTERRUPTED   	= 3; // Connection interrupted or cancelled
    public static final int CONN_TIMEOUT       	= 4; // Connection establishment timeout
    public static final int CONN_SSL_ERROR      = 5; // SSL/TLS handshake or certificate failure

    public static final int SUCCESS            	= 200; // Success / OK

    public static final int CLIENT_ERROR       	= 400; // Generic client-side error
    public static final int CLIENT_UNAUTHORIZED = 401; // Authentication or permission failure
    public static final int CLIENT_TIMEOUT    	= 408; // Client-side operation timeout
    public static final int CLIENT_CONFLICT     = 409; // Duplicate key / Constraint violation

    public static final int SERVER_ERROR      	= 500; // Generic server/remote error
    public static final int SERVER_TIMEOUT     	= 504; // Server/Gateway response timeout
	
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	private TraceUpdate trace;
	private Instant start;
	private final TraceHub hub;
	
	protected StatefulExecutionListener() {
		this.hub = hub();
	}
	
	protected abstract TraceSignal signal(Instant start, T cnx) throws Exception;
	
	protected abstract TraceUpdate update(TraceSignal signal) throws Exception;
	
	protected ExceptionTrace exception(Throwable t, int order) {
		var upd = getTrace();
		var root = rootCauseException(t);
		if(upd.getStatus() < 0) {
			upd.setStatus(resolveStatus(root));
		}
		var ex = fromException(root, 0, 0);
		ex.setOffset(order);
		ex.setTraceId(upd.getId());
		return ex;
	}
	
	protected int resolveStatus(Throwable t) {
	    
		return switch (t) {
	        case java.net.UnknownHostException e -> CONN_UNKNOWN_HOST;
	        case java.nio.channels.UnresolvedAddressException e-> CONN_UNKNOWN_HOST;
	        case java.net.ConnectException e -> CONN_REFUSED;
	        case java.net.NoRouteToHostException e -> CONN_REFUSED;
	        case java.net.BindException e -> CONN_REFUSED;
	        
	        case java.net.SocketTimeoutException e -> nonNull(e.getMessage()) && e.getMessage().contains("connect") ? CONN_TIMEOUT : SERVER_TIMEOUT;
	        case java.util.concurrent.TimeoutException e -> CLIENT_TIMEOUT;
	        
	        case java.io.InterruptedIOException e -> CONN_INTERRUPTED;
	        case java.lang.InterruptedException e -> CONN_INTERRUPTED;
	        case java.util.concurrent.CancellationException e -> CONN_INTERRUPTED;
	        
	        case javax.net.ssl.SSLException e-> CONN_SSL_ERROR; 
	        
	        case java.io.IOException e -> CONN_ERROR;

	        default -> SERVER_ERROR;
	    };
	}
	
	public ExecutionListener<T> connectionListener(StageBuilder<T> stgBuilder) {
		return connectionListener(stgBuilder, identity());
	}
	
	public <U> ExecutionListener<U> connectionListener(StageBuilder<T> stgBuilder, Function<U, T> mapper) {
		if(nonNull(trace) || nonNull(start) || stageCounter.get() > 0) {
			report("connectionListener", "listener was not reset");
			reset();
		}
		return (s,e,o,t)-> {
			this.start = s;
			var cnx = mapper.apply(o);
			var sgn = signal(s, cnx);
			if(nonNull(sgn)) {
				this.hub.emitTrace(sgn);
				this.trace = update(sgn);
				this.trace.setStatus(-1); //initial status
				if(nonNull(stgBuilder)) {
					stageListener(stgBuilder).safeHandle(s, e, cnx, t);
				}
				if(nonNull(t)) { // if connection error
					disconnectionListener(null).safeHandle(s, e, cnx, t);
				}
			}
			else {
				reportTraceIsNull("connectionListener");
			}
		}; 
	}
	
	public <R> ExecutionListener<R> stageListener(StageBuilder<R> stgBuilder){
		return (s,e,o,t)-> {
			if(nonNull(trace)) {
				var stg = stgBuilder.newStage(s, e, o, t);
				if(nonNull(stg)) {
					this.hub.emitTrace(stg);
					if(nonNull(t)) {
						var ex = exception(t, stg.getOrder());
						if(nonNull(ex)) {
							this.hub.emitTrace(ex);
						}
					}
				}
			}
			else {
				reportTraceIsNull("stageListener");
			}
		};
	}
	
	public <R> ExecutionListener<R> disconnectionListener(StageBuilder<R> stgBuilder) {
		return (s,e,o,t)-> {
			try {
				if(nonNull(trace)) {
					if(nonNull(stgBuilder)) {
						stageListener(stgBuilder).safeHandle(s, e, o, t);
					}
					if(trace.getStatus() < 0) {
						trace.setStatus(SUCCESS);
					}
					trace.setEnd(e);
					this.hub.emitTrace(trace);
				}
				else {
					reportTraceIsNull("disconnectionListener");
				}
			}
			finally {
				reset();
			}
		};
	}
	
	void reset() {
		this.stageCounter.set(0);
		this.start = null;
		this.trace = null;
	}

	protected void reportTraceIsNull(String action) {
		report(action, "trace is null");
	}
	
	protected void report(String action, String msg) {
		this.hub.reportMessage(true, this.getClass().getSimpleName() + "." + action, msg);
	}
}
