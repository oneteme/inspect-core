package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.fromException;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;
import org.usf.inspect.core.SafeCallable.SafeConsumer;
import org.usf.inspect.core.SafeCallable.SafeFunction;

/**
 * 
 * @author u$f
 *
 */
public interface Monitor {
	
	static final String EXECUTION_HANDLER_ACTION = "Monitor.executionHandler";
	static final String CONNECTION_HANDLER_ACTION = "Monitor.connectionHandler";

	static <R> ExecutionListener<R> traceAroundHttp(HttpSession2 session, SafeConsumer<HttpSession2> preProcess) {
		return traceAround(session, HttpSession2::createCallback, preProcess, null);
	}

	static <R> ExecutionListener<R> traceAroundHttp(HttpSession2 session, SafeConsumer<HttpSession2> preProcess, BiConsumer<HttpSessionCallback, R> postProcess) {
		return traceAround(session, HttpSession2::createCallback, preProcess, postProcess);
	}

	static <R> ExecutionListener<R> traceAroundMethod(MainSession2 session, SafeConsumer<MainSession2> preProcess) {
		return traceAround(session, MainSession2::createCallback, preProcess, null);
	}
	
	static <R> ExecutionListener<R> traceAroundMethod(MainSession2 session, SafeConsumer<MainSession2> preProcess, BiConsumer<MainSessionCallback, R> postProcess) {
		return traceAround(session, MainSession2::createCallback, preProcess, postProcess);
	}

	static <R> ExecutionListener<R> traceAroundMethod(LocalRequest2 request, SafeConsumer<LocalRequest2> preProcess) {
		return traceAround(request, LocalRequest2::createCallback, preProcess, null);
	}
	
	static <T extends Initializer, U extends Callback & AtomicTrace, R> ExecutionListener<R> traceAround(T session, Function<T, U> callbackFn, SafeConsumer<T> preProcess, BiConsumer<U, R> postProcess) {
		try {
			if(nonNull(preProcess)) {
				preProcess.accept(session);
			}
			context().emitTrace(session);
		}
		catch (Exception e) {
			context().reportError(true, EXECUTION_HANDLER_ACTION, e);
		}
		var callback = callbackFn.apply(session); 
		if(callback instanceof AbstractSessionCallback ses) {
			setActiveContext(ses);
		}
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, EXECUTION_HANDLER_ACTION)) {
				if(nonNull(postProcess)) {
					try {
						postProcess.accept(callback, o);
					}
					catch (Exception ex) {
						context().reportError(true, EXECUTION_HANDLER_ACTION, ex);
					}
				}
				callback.setStart(s); //nullable
				if(nonNull(t)) {
					callback.setException(fromException(t));
				}
				callback.setEnd(e);
				context().emitTrace(callback);
			}
			if(callback instanceof AbstractSessionCallback ses) {
				clearContext(ses);
			}
		};
	}
	
	static <T extends AbstractRequest2, U extends AbstractRequestCallback, R> ExecutionListener<R> traceBegin(SafeFunction<Instant, T> factory, Function<T, U> callbackFn, SafeBiConsumer<T, R> preProcess){
		return (s,e,o,t)-> {
			var session = factory.apply(s); //cannot be null
			if(nonNull(preProcess)) {
				try {
					preProcess.accept(session, o);
				}
				catch (Exception ex) {
					context().reportError(true, "Monitor.traceBegin", ex);
				}
			}
			context().emitTrace(session);
			var callback = callbackFn.apply(session); //cannot be null
			if(nonNull(t)) { // if connection error
				callback.setEnd(e);
				context().emitTrace(callback);
			}
		};
	}
	
	static <U extends Callback, R> ExecutionListener<R> traceEnd(U callback){
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, "Monitor.traceEnd")) {
				callback.setEnd(e);
				context().emitTrace(callback);
			}
		};
	}

	static <U extends Callback, R> ExecutionListener<R> traceStep(U callback, StageCreator<R> stageFn){
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, "Monitor.traceStep")) {
				var stg = stageFn.createStage(s, e, o, t);
				if(nonNull(stg)) {
					context().emitTrace(stg);
				}
			}
		};
	}
	
	static boolean assertStillOpened(Callback callback, String action) {
		if(nonNull(callback)) {
			if(isNull(callback.getEnd())) {
				return true;
			}
			context().reportMessage(true, action, format("'%s.end' is not null", callback.getClass().getSimpleName()));
		}
		context().reportMessage(true, action, "callback is null");
		return false;
	}

	static boolean assertMonitorNonNull(Object monitor, String action) {
		if (isNull(monitor)) {
			context().reportMessage(false, action, "monitor is null");
			return false;
		}
		return true;
	}
	
	interface StageCreator<R> {
		
		AbstractStage createStage(Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
