package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public abstract class ConnectionLifecycleTracer implements DualEventTracer {
	
	private final AtomicInteger stageCounter = new AtomicInteger();

	private TraceUpdate update;
	
	protected abstract TraceSignal signal(Instant start);
	
	protected abstract TraceUpdate update(TraceSignal signal);
	
	@Override
	public Throwable mapException(Throwable t) {
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
		if(nonNull(update) || stageCounter.get() > 0) {
			hub().reportMessage(true, "ConnectionLifecycleTracer.connectionListener", "tracer was not reset");
			reset();
		}
		return (s,e,o,t)-> {
			var sgn = signal(s);
			try {
				cons.accept(sgn, o);
			}
			catch (Exception ex) {
				hub().reportError(true, "ConnectionLifecycleTracer.connectionListener", ex);
			}
			hub().emitTrace(sgn);
			this.update = update(sgn);
			this.update.setStatus(-1); //initial status
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
			if(assertActiveTraceUpdate("ConnectionLifecycleTracer.stageListener")) {
				var stg = stgBuilder.newStage(s, e, o, t);
				if(nonNull(stg)) {
					hub().emitTrace(stg);
					if(nonNull(t)) {
						t = mapException(t);
						if(update.getStatus() < 0) {
							update.setStatus(resolveStatus(t));
						}
						var exp = exceptionTrace(t, stg.getOrder());
						hub().emitTrace(exp);
					}
				}
			}
		};
	}
	
	public <R> ExecutionListener<R> disconnectionListener(StageBuilder<R> stgBuilder) {
		return (s,e,o,t)-> {
			if(assertActiveTraceUpdate("ConnectionLifecycleTracer.disconnectionListener")) {
				if(nonNull(stgBuilder)) {
					stageListener(stgBuilder).safeHandle(s, e, o, t);
				}
				if(update.getStatus() < 0) {
					update.setStatus(SUCCESS);
				}
				update.setEnd(e);
				hub().emitTrace(update);
			}
			reset();
		};
	}
	
	void reset() {
		this.stageCounter.set(0);
		this.update = null;
	}
}
