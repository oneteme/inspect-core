package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StageBuilder;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;

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
public abstract class StatefulExecutionListener implements Monitor2 {
	
	private final AtomicInteger stageCounter = new AtomicInteger();
	
	private TraceUpdate trace;
	private Instant start;
	
	protected ExceptionTrace exception(Throwable t, int order) {
		var upd = getTrace();
		var root = exception(t);
		if(upd.getStatus() < 0) {
			upd.setStatus(resolveStatus(root));
		}
		var ex = fromException(root, 0, 0);
		ex.setOffset(order);
		ex.setTraceId(upd.getId());
		return ex;
	}
	
	@Override
	public Throwable exception(Throwable t) {
		return rootCauseException(t);
	}
	
	@Override
	public int resolveStatus(Throwable t) {
	    
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
	
	public <T> ExecutionListener<T> connectionListener(StageBuilder<T> stgBuilder, SafeBiConsumer<TraceSignal,T> cons) {
		if(nonNull(trace) || nonNull(start) || stageCounter.get() > 0) {
			report("connectionListener", "listener was not reset");
			reset();
		}
		return (s,e,o,t)-> {
			this.start = s;
			var sgn = signal(s);
			try {
				cons.accept(sgn, o);
			}
			catch (Exception ex) {
				hub().reportError(true, this.getClass() + ".connectionListener", ex);
			}
			hub().emitTrace(sgn);
			this.trace = update(sgn);
			this.trace.setStatus(-1); //initial status
			if(nonNull(stgBuilder)) {
				stageListener(stgBuilder).safeHandle(s, e, o, t);
			}
			if(nonNull(t)) { // if connection error
				disconnectionListener(null).safeHandle(s, e, o, t);
			}
		}; 
	}
	
	public <R> ExecutionListener<R> stageListener(StageBuilder<R> stgBuilder){
		return (s,e,o,t)-> {
			if(nonNull(trace)) {
				var stg = stgBuilder.newStage(s, e, o, t);
				if(nonNull(stg)) {
					hub().emitTrace(stg);
					if(nonNull(t)) {
						var ex = exception(t, stg.getOrder());
						if(nonNull(ex)) {
							hub().emitTrace(ex);
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
					hub().emitTrace(trace);
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
		hub().reportMessage(true, this.getClass().getSimpleName() + "." + action, msg);
	}
}
