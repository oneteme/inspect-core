package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeConsumer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter(value = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AtomicExecutionListener<T extends TraceSignal> implements Monitor2 {

	public <R> ExecutionListener<R> executionListener(SafeConsumer<T> cons) {
		return executionListener(systemUTC().instant(), cons);
	}
	
	public <R> ExecutionListener<R> executionListener(Instant start, SafeConsumer<T> cons) {
		var upd = traceUpdate(start, cons);
		return executionListener(upd);
	}
	
	public <R> ModifiableExecutionListener<R> modifiableExecutionListener(SafeConsumer<T> cons) {
		return modifiableExecutionListener(systemUTC().instant(), cons);
	}
	
	public <R> ModifiableExecutionListener<R> modifiableExecutionListener(Instant start, SafeConsumer<T> cons) {
		var upd = traceUpdate(start, cons);
		return new ModifiableExecutionListener<>(upd, executionListener(upd));
	}
	
	@SuppressWarnings("unchecked")
	TraceUpdate traceUpdate(Instant start, SafeConsumer<T> cons) {
		var sgn = (T) signal(start);
		try {
			cons.accept(sgn);
		}
		catch (Exception e) {
			hub().reportError(true, this.getClass() + ".executionListener", e);
		}
		hub().emitTrace(sgn);
		return update(sgn);
	}
	
	<R> ExecutionListener<R> executionListener(TraceUpdate upd) {
		if(upd instanceof AbstractSessionUpdate ctx) {
			setActiveContext(ctx);
		}
		return (s,e,o,t)-> {
			if(nonNull(upd)) {
				upd.setStart(s); //real method start
				if(nonNull(t)) {
					t = exception(t);
					hub().emitTrace(fromException(t));
					upd.setStatus(resolveStatus(t));
				}
				else {
					upd.setStatus(SUCCESS);
				}
				upd.setEnd(e);
				hub().emitTrace(upd);
				if(upd instanceof AbstractSessionUpdate ctx) {
					clearContext(ctx);
				}
			}
			else {
				hub().reportMessage(true, this.getClass() + "executionListener", "update is null");
			}
		};
	}
}
