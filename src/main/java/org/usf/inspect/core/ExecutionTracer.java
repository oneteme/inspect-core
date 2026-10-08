package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;
import static org.usf.inspect.core.SessionContextManager.createMainSession;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;
import org.usf.inspect.core.SafeCallable.SafeConsumer;

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
		if(update instanceof AbstractSessionUpdate session) {
			setActiveContext(session);
		}
		this.update.setStatus(UNKNOWN); //initial status
	}
	
	@Override
	public void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception {
		if(assertActiveTraceUpdate("ExecutionTracer.handle")) {
			update.setStart(start); //real method start
			if(nonNull(thrw)) {
				thrw = mapException(thrw);
				if(update.getStatus() < 0) {
					update.setStatus(resolveStatus(thrw));
				}
				hub().emitExceptionTrace(thrw, update, end.toEpochMilli());
			}
			else if(update.getStatus() < 0) {
				update.setStatus(SUCCESS);
			}
			update.setEnd(end);
			if(update instanceof AbstractSessionUpdate session) {
				var ctx = clearContext(session);
				if(nonNull(ctx)) {
					ctx.updateAsync(); //initial duration
				}
			}
			hub().emitTrace(update);
		}
	}

	public ExecutionListener<T> map(SafeBiConsumer<TraceUpdate, T> cons) {
		return (s,e,o,t)->{
			if(assertActiveTraceUpdate("ExecutionTracer.map")) {
				try {
					cons.accept(update, o); //execute before
				}
				catch (Exception ex) {
					hub().emitReport("ExecutionTracer.map", ex);
				}
				handle(s, e, o, t);
			}
		};
	}
	
	public static <R> ExecutionTracer<R> forLocalRequest(Instant start, SafeConsumer<LocalRequestSignal> cons) {
		var sgn = createLocalRequest(start);
		traceSignal(sgn, cons, "ExecutionTracer.forLocalRequest");
		return new ExecutionTracer<>(new LocalRequestUpdate(sgn.getId()));		
	}
	
	public static <R> ExecutionTracer<R> forMainSession(Instant start, MainSessionType type, SafeConsumer<MainSessionSignal> cons) {
		var sgn = createMainSession(type, start);
		traceSignal(sgn, cons, "ExecutionTracer.forMainSession");
		return new ExecutionTracer<>(new MainSessionUpdate(sgn.getId()));		
	}
	
	public static <R> ExecutionTracer<R> forHttpSession(HttpSessionSignal sgn, SafeConsumer<HttpSessionSignal> cons) {
		traceSignal(sgn, cons, "ExecutionTracer.forHttpSession");
		return new ExecutionTracer<>(new HttpSessionUpdate(sgn.getId()));		
	}
	
	protected static <T extends TraceSignal> void traceSignal(T sgn, SafeConsumer<? super T> supp, String action) {
		try {
			supp.accept(sgn);
		}
		catch (Exception e) {
			hub().emitReport(action, e);
		}
		hub().emitTrace(sgn);
	}
}
