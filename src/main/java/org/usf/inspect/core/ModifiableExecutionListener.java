package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Instant;

import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.SafeCallable.SafeConsumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ModifiableExecutionListener<T> implements ExecutionListener<T> {
	
	private final TraceUpdate update;
	private final ExecutionListener<T> listener;
	
	public void updateTrace(SafeConsumer<TraceUpdate> cons) {
		if(nonNull(update)) {
			try {
				cons.accept(update);
			}
			catch (Exception e) {
				hub().reportError(true, "updateTrace", e);
			}
		}
		else {
			hub().reportMessage(true, "updateTrace", "update is null");
		}
	}

	@Override
	public void handle(Instant start, Instant end, T obj, Throwable thrw) throws Exception {
		listener.handle(start, end, obj, thrw);
	}
	
}
