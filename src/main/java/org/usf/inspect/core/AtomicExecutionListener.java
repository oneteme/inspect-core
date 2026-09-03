package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
public abstract class AtomicExecutionListener implements Monitor2 {
	
	public <R> ExecutionListener<R> executionListener(SafeConsumer<TraceSignal> cons) {
		return executionListener(systemUTC().instant(), cons);
	}
	
	public <R> ExecutionListener<R> executionListener(Instant start, SafeConsumer<TraceSignal> cons) {
		return executionListener(createSignal(start, cons));
	}
	
	public <R> ModifiableExecutionListener<R> modifiableExecutionListener(Instant start, SafeConsumer<TraceSignal> cons) {
		var sgn = createSignal(start, cons);
		hub().emitTrace(sgn);
		var upd = update(sgn);
		return new ModifiableExecutionListener<>(upd, executionListener(upd));
	}
	
	TraceSignal createSignal(Instant start, SafeConsumer<TraceSignal> cons) {
		var sgn = signal(start);
		try {
			cons.accept(sgn);
		}
		catch (Exception e) {
			hub().reportError(true, this.getClass() + ".createSignal", e);
		}
		return sgn;
	}

	<R> ExecutionListener<R> executionListener(TraceSignal sgn) {
		hub().emitTrace(sgn);
		return executionListener(update(sgn));
	}
	
	<R> ExecutionListener<R> executionListener(TraceUpdate upd) {
		if(upd instanceof AbstractSessionUpdate ctx) {
			setActiveContext(ctx);
		}
		return isNull(upd) ? Monitor2.noUpdateExecutionListenner() : (s,e,o,t)-> {
			upd.setStart(s); //real method start
			if(nonNull(t)) {
				t = exception(t);
				upd.setStatus(resolveStatus(t));
				var exp = exceptionTrace(t, upd, e.toEpochMilli());
				hub().emitTrace(exp);
			}
			else {
				upd.setStatus(SUCCESS);
			}
			upd.setEnd(e);
			hub().emitTrace(upd);
			if(upd instanceof AbstractSessionUpdate ctx) {
				clearContext(ctx);
			}
		};
	}
}
