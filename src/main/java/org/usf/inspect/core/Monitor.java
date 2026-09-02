package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.StatefulExecutionListener.SERVER_ERROR;
import static org.usf.inspect.core.StatefulExecutionListener.SUCCESS;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeConsumer;

/**
 * 
 * @author u$f
 *
 */
public interface Monitor {
	
	static final String TRACE_ATOMIC_ACTION = "Monitor.traceAtomic";

	
	static <R> ExecutionListener<R> traceAroundMethod(MainSessionSignal session, SafeConsumer<MainSessionSignal> preProcess, BiConsumer<MainSessionUpdate, R> postProcess) {
		return traceAtomic(session, MainSessionSignal::createCallback, preProcess, postProcess);
	}

	static <T extends TraceSignal, U extends TraceUpdate & AtomicTrace, R> ExecutionListener<R> traceAtomic(T signal, Function<T, U> callbackFn, SafeConsumer<T> preProcess, BiConsumer<U, R> postProcess) {
		try {
			if(nonNull(preProcess)) {
				preProcess.accept(signal);
			}
			hub().emitTrace(signal);
		}
		catch (Exception e) {
			hub().reportError(true, TRACE_ATOMIC_ACTION, e);
		}
		var callback = callbackFn.apply(signal); 
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
					hub().emitTrace(fromException(t));
					callback.setStatus(SERVER_ERROR);
				}
				else {
					callback.setStatus(SUCCESS);
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
			hub().reportMessage(true, action, format("'%s.end' is not null", callback.getClass().getSimpleName()));
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
	
	public interface StageBuilder<R> {
		
		AbstractStage newStage(Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
