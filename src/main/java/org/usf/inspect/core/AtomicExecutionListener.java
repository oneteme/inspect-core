package org.usf.inspect.core;

import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeBiConsumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ModifiableExecutionListener<T> implements ExecutionListener<T> {
	
	private final TraceUpdate update;
	private final ExecutionListener<T> listener;
	
	public ExecutionListener<T> map(SafeBiConsumer<TraceUpdate, T> cons) {
		return (s,e,o,t)->{
			try {
				cons.accept(update, o); //execute before
			}
			catch (Exception ex) {
				hub().reportError(true, "ModifiableExecutionListener.map", ex);
			}
			handle(s, e, o, t);
		};
	}

	@Override
	public void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception {
		listener.handle(start, end, obj, thrw);
	}
}
