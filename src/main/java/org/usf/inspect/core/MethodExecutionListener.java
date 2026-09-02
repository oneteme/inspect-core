package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.rootCauseException;
import static org.usf.inspect.core.SessionContextManager.createLocalRequest;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public final class MethodExecutionListener extends AtomicExecutionListener<LocalRequestSignal> {

	@Override
	protected LocalRequestSignal signal(Instant start) {
		return createLocalRequest(start);
	}
	
	@Override
	protected TraceUpdate update(TraceSignal signal) {
		return new LocalRequestUpdate(signal.getId());
	}
	
	@Override
	protected Throwable mapException(Throwable e) {
		return rootCauseException(e);
	}
}
