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

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
public interface Monitor {
	
	static final String EXECUTION_HANDLER_ACTION = "Monitor.executionHandler";

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

	static boolean assertStillOpened(Callback callback, String action) {
		if(nonNull(callback)) {
			if(isNull(callback.getEnd())) {
				return true;
			}
			context().reportMessage(true, action, format("'%s.end' is null", callback.getClass().getSimpleName()));
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
	
	public abstract class StatefulMonitor<T extends Initializer, V extends Callback> {
		
		@Getter(AccessLevel.PROTECTED) 
		private V callback; //make it private with accessor?
		
		protected abstract V createCallback(T session);

		protected <R> ExecutionListener<R> traceBegin(Function<Instant, T> sessionFn, SafeBiConsumer<T, R> preProcess, ExecutionListener<? super R> after){
			return (s,e,o,t)-> {
				var session = sessionFn.apply(s); //session cannot be null
				try {
					preProcess.accept(session, o);
				}
				catch (Exception ex) {
					context().reportError(true, "StatefulMonitor.traceBegin", ex);
				}
				context().emitTrace(session);
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
				if(assertStillOpened(callback, "StatefulMonitor.traceStep")) {
					var stg = stageFn.createStage(s, e, o, t);
					if(nonNull(stg)) {
						context().emitTrace(stg);
					}
				}
			};
		}
		
		protected <R> ExecutionListener<R> traceEnd(ExecutionListener<? super R> after){
			return (s,e,o,t)-> {
				if(assertStillOpened(callback, "StatefulMonitor.traceEnd")) {
					if(nonNull(after)) {
						after.safeHandle(s, e, o, t);
					}
					callback.setEnd(e);
					context().emitTrace(callback);
				}
			};
		}
	}
	
	interface StageCreator<R> {
		
		AbstractStage createStage(Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
