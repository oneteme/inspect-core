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
import java.util.function.Consumer;
import java.util.function.Function;

import org.usf.inspect.core.ExecutionMonitor.ExecutionHandler;

public interface Monitor {
	
	static final String EXECUTION_HANDLER_ACTION = "Monitor.executionHandler";
	
	default void emit(EventTrace trace) {
		try {
			context().emitTrace(trace);
		}
		catch (Throwable ex) {// do not throw exception
			context().reportError(true, "EventTrace.emit", ex);
		}
	}
	
	default void reportError(boolean stack, String action, Throwable thwr) {
		context().reportError(stack, action, thwr);
	}

	default void reportMessage(boolean stack, String action, String msg) {
		context().reportMessage(stack, action, msg);
	}

	static <R> ExecutionHandler<R> httpExecutionHandler(HttpSession2 session, Consumer<HttpSession2> preProcess) {
		return executionHandler(session, HttpSession2::createCallback, preProcess, null);
	}

	static <R> ExecutionHandler<R> httpExecutionHandler(HttpSession2 session, Consumer<HttpSession2> preProcess, BiConsumer<HttpSessionCallback, R> postProcess) {
		return executionHandler(session, HttpSession2::createCallback, preProcess, postProcess);
	}

	static <R> ExecutionHandler<R> mainExecutionHandler(MainSession2 session, Consumer<MainSession2> preProcess) {
		return executionHandler(session, MainSession2::createCallback, preProcess, null);
	}
	
	static <R> ExecutionHandler<R> mainExecutionHandler(MainSession2 session, Consumer<MainSession2> preProcess, BiConsumer<MainSessionCallback, R> postProcess) {
		return executionHandler(session, MainSession2::createCallback, preProcess, postProcess);
	}
	
	static <T extends AbstractSession2, U extends AbstractSessionCallback, R> ExecutionHandler<R> executionHandler(T session, Function<T, U> callbackFn, Consumer<T> preProcess, BiConsumer<U, R> postProcess) {
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
		setActiveContext(callback);
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, EXECUTION_HANDLER_ACTION)) {
				try {
					callback.setStart(s); //nullable
					if(nonNull(t)) {
						callback.setException(fromException(t));
					}
					callback.setEnd(e);
					if(nonNull(postProcess)) {
						postProcess.accept(callback, o);
					}
					context().emitTrace(callback);
				}
				catch (Exception ex) {
					context().reportError(true, EXECUTION_HANDLER_ACTION, ex);
				}
			}
			if(nonNull(callback)) {
				clearContext(callback);
			}
		};
	}
	
	static <R> ExecutionHandler<R> executionHandler(LocalRequest2 request, Consumer<LocalRequest2> preProcess) {
		return executionHandler(request, preProcess, null);
	}
	
	static <R> ExecutionHandler<R> executionHandler(LocalRequest2 request, Consumer<LocalRequest2> preProcess, BiConsumer<LocalRequestCallback, R> postProcess) {
		try {
			if(nonNull(preProcess)) {
				preProcess.accept(request);
			}
			context().emitTrace(request);
		}
		catch (Exception e) {
			context().reportError(true, EXECUTION_HANDLER_ACTION, e);
		}
		var callback = request.createCallback(); 
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, EXECUTION_HANDLER_ACTION)) {
				try {
					callback.setStart(s); //nullable
					if(nonNull(t)) {
						callback.setException(fromException(t));
					}
					callback.setEnd(e);
					if(nonNull(postProcess)) {
						postProcess.accept(callback, o);
					}
					context().emitTrace(callback);
				}
				catch (Exception ex) {
					context().reportError(true, EXECUTION_HANDLER_ACTION, ex);
				}
			}
		};
	}
	
	static <T extends AbstractRequest2, U extends AbstractRequestCallback, R> ExecutionHandler<R> connectionHandler(Function<Instant, T> factory, Function<T, U> callbackFn, BiConsumer<T, R> preProcess, StageCreator<U,R> stageFn) {
		return (s,e,o,t)-> {
			var session = factory.apply(s); //cannot be null
			try {
				if(nonNull(preProcess)) {
					preProcess.accept(session, o);
				}
				context().emitTrace(session);
			}
			catch (Exception ex) {
				context().reportError(true, EXECUTION_HANDLER_ACTION, ex);
			}
			try {
				var callback = callbackFn.apply(session); //cannot be null
				if(nonNull(stageFn)) {
					connectionStageHandler(callback, stageFn);
				}
				if(nonNull(t)) { // if connection error
					callback.setEnd(e);
					context().emitTrace(callback);
				}
			}
			catch (Exception ex) {
				context().reportError(true, EXECUTION_HANDLER_ACTION, ex);
			}
		};
	}
	
	static <U extends Callback, R> ExecutionHandler<R> disconnectionHandler(U callback, StageCreator<U,R> stageFn){
		return (s,e,o,t)-> {
			if(assertStillOpened(callback, EXECUTION_HANDLER_ACTION)) {
				if(nonNull(stageFn)) {
					connectionStageHandler(callback, stageFn);
				}
				try {
					callback.setEnd(e);
					context().emitTrace(callback);
				}
				catch (Exception ex) {
					context().reportError(true, "Monitor.disconnectionHandler", ex);
				}
			}
		};
	}

	static <U extends Callback, R> ExecutionHandler<R> connectionStageHandler(U callback, StageCreator<U,R> stageFn){
		return (s,e,o,t)-> {
			if(nonNull(stageFn) && assertStillOpened(callback, EXECUTION_HANDLER_ACTION)) {
				try {
					var stage = stageFn.createStage(callback, s, e, o, t);
					if(nonNull(stage)) {
						context().emitTrace(stage);
					}
				}
				catch (Exception ex) {
					context().reportError(true, "Monitor.stageHandler", ex);
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
	
	interface StageCreator<U extends Callback, R> {
		AbstractStage createStage(U callback, Instant start, Instant end, R obj, Throwable thrw) throws Exception;
	}
}
