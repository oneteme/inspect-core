package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;
import org.usf.inspect.core.SafeCallable.SafeConsumer;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public interface Monitor {
	
	static final String TRACE_ATOMIC_ACTION = "Monitor.traceAtomic";

	static <R> ExecutionListener<R> traceAroundHttp(HttpSessionSignal session, SafeConsumer<HttpSessionSignal> preProcess) {
		return traceAtomic(session, HttpSessionSignal::createCallback, preProcess, null);
	}

	static <R> ExecutionListener<R> traceAroundHttp(HttpSessionSignal session, SafeConsumer<HttpSessionSignal> preProcess, BiConsumer<HttpSessionUpdate, R> postProcess) {
		return traceAtomic(session, HttpSessionSignal::createCallback, preProcess, postProcess);
	}

	static <R> ExecutionListener<R> traceAroundMethod(MainSessionSignal session, SafeConsumer<MainSessionSignal> preProcess) {
		return traceAtomic(session, MainSessionSignal::createCallback, preProcess, null);
	}
	
	static <R> ExecutionListener<R> traceAroundMethod(MainSessionSignal session, SafeConsumer<MainSessionSignal> preProcess, BiConsumer<MainSessionUpdate, R> postProcess) {
		return traceAtomic(session, MainSessionSignal::createCallback, preProcess, postProcess);
	}

	static <R> ExecutionListener<R> traceAroundMethod(LocalRequestSignal request, SafeConsumer<LocalRequestSignal> preProcess) {
		return traceAtomic(request, LocalRequestSignal::createCallback, preProcess, null);
	}
	
	static <T extends TraceSignal, U extends TraceUpdate & AtomicTrace, R> ExecutionListener<R> traceAtomic(T session, Function<T, U> callbackFn, SafeConsumer<T> preProcess, BiConsumer<U, R> postProcess) {
		try {
			if(nonNull(preProcess)) {
				preProcess.accept(session);
			}
			hub().emitTrace(session);
		}
		catch (Exception e) {
			hub().reportError(true, TRACE_ATOMIC_ACTION, e);
		}
		var callback = callbackFn.apply(session); 
		if(callback instanceof AbstractSessionUpdate ctx) {
			setActiveContext(ctx);
		}
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, TRACE_ATOMIC_ACTION)) {
				if(nonNull(postProcess)) {
					try {
						postProcess.accept(callback, o);
					}
					catch (Exception ex) {
						hub().reportError(true, TRACE_ATOMIC_ACTION, ex);
					}
				}
				callback.setStart(s); //nullable
				if(nonNull(t)) {
					callback.setException(fromException(t));
				}
				callback.setEnd(e);
				hub().emitTrace(callback);
			}
			if(callback instanceof AbstractSessionUpdate ctx) {
				clearContext(ctx);
			}
		};
	}

	static boolean assertStillOpened(TraceUpdate callback, String action) {
		if(nonNull(callback)) {
			if(isNull(callback.getEnd())) {
				return true;
			}
			hub().reportMessage(true, action, format("'%s.end' is null", callback.getClass().getSimpleName()));
		}
		else {
			hub().reportMessage(true, action, "callback is null");
		}
		return false;
	}

	static boolean assertMonitorNonNull(Object monitor, String action) {
		if (isNull(monitor)) {
			hub().reportMessage(false, action, "monitor is null");
			return false;
		}
		return true;
	}
	
	public abstract class StatefulMonitor<T extends TraceSignal, V extends TraceUpdate> {
		
		@Getter(AccessLevel.PROTECTED) 
		private V callback;
		
		protected abstract V createCallback(T session);

		protected <R> ExecutionListener<R> traceBegin(Function<Instant, T> sessionFn, SafeBiConsumer<T, R> preProcess, ExecutionListener<? super R> after){
			return (s,e,o,t)-> {
				var session = sessionFn.apply(s); //session cannot be null
				try {
					preProcess.accept(session, o);
				}
				catch (Exception ex) {
					hub().reportError(true, this.getClass().getSimpleName() + ".traceBegin", ex);
				}
				hub().emitTrace(session);
				callback = createCallback(session); //cannot be null
				if(nonNull(after)) {
					after.safeHandle(s, e, o, t);
				}
				if(nonNull(t)) { // if connection error
					traceEnd(null).handle(s, e, null, t);
				}
			};
		}
		
		protected <R> ExecutionListener<R> traceStep(StageCreator<R> stageFn){
			return (s,e,o,t)-> {
				if(assertStillOpened(callback, this.getClass().getSimpleName() + ".traceStep")) {
					var stg = stageFn.createStage(s, e, o, t);
					if(nonNull(stg)) {
						hub().emitTrace(stg);
					}
				}
			};
		}
		
		protected <R> ExecutionListener<R> traceEnd(ExecutionListener<? super R> after){
			return (s,e,o,t)-> {
				if(assertStillOpened(callback, this.getClass().getSimpleName() + ".traceEnd")) {
					if(nonNull(after)) {
						after.safeHandle(s, e, o, t);
					}
					callback.setEnd(e);
					hub().emitTrace(callback);
				}
			};
		}
	}
	
	interface StageCreator<R> {
		
		AbstractStage createStage(Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
