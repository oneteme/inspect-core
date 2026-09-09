package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;
import org.usf.inspect.core.SafeCallable.SafeSupplier;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
@Getter
public class ExecutionTracer<T> implements ExecutionListener<T>, DualEventTracer {

	private final TraceUpdate update; //may be null

	public ExecutionTracer(TraceUpdate update) {
		this.update = update;
		if(update instanceof AbstractSessionUpdate ctx) {
			setActiveContext(ctx);
		}
	}
	
	@Override
	public void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception {
		if(assertActiveTraceUpdate("ExecutionTracer.handle")) {
			update.setStart(start); //real method start
			if(nonNull(thrw)) {
				thrw = mapException(thrw);
				update.setStatus(resolveStatus(thrw));
				var exp = exceptionTrace(thrw, end.toEpochMilli());
				hub().emitTrace(exp);
			}
			else {
				update.setStatus(SUCCESS);
			}
			update.setEnd(end);
			hub().emitTrace(update);
			if(update instanceof AbstractSessionUpdate ctx) {
				clearContext(ctx);
			}
		}
	}

	public ExecutionListener<T> map(SafeBiConsumer<TraceUpdate, T> cons) {
		return (s,e,o,t)->{
			if(assertActiveTraceUpdate("ExecutionTracer.map")) {
				try {
					cons.accept(update, o); //execute before
				}
				catch (Exception ex) {
					hub().reportError("ExecutionTracer.map", ex);
				}
				handle(s, e, o, t);
			}
		};
	}
	
	public static <R> ExecutionTracer<R> forLocalRequest(SafeSupplier<LocalRequestSignal> cons) {
		var sgn = traceSignal(cons);
		return new ExecutionTracer<>(new LocalRequestUpdate(nonNull(sgn) ? sgn.getId() : null));		
	}
	
	public static <R> ExecutionTracer<R> forMainSession(SafeSupplier<MainSessionSignal> cons) {
		var sgn = traceSignal(cons);
		return new ExecutionTracer<>(new MainSessionUpdate(nonNull(sgn) ? sgn.getId() : null));		
	}
	
	public static <R> ExecutionTracer<R> forHttpSession(SafeSupplier<HttpSessionSignal> cons) {
		var sgn = traceSignal(cons);
		return new ExecutionTracer<>(new HttpSessionUpdate(nonNull(sgn) ? sgn.getId() : null));		
	}
	
	protected static TraceSignal traceSignal(SafeSupplier<? extends TraceSignal> supp) {
		TraceSignal sgn = null;
		try {
			sgn = supp.get();
		}
		catch (Exception e) {
			hub().reportError("ExecutionTracer.traceSignal", e);
		}
		if(nonNull(sgn)) {
			hub().emitTrace(sgn);
		}
		return sgn;
	}
}
