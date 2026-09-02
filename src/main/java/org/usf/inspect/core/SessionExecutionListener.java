package org.usf.inspect.core;

import static org.usf.inspect.core.SessionContextManager.createBatchSession;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public final class SessionExecutionListener extends AtomicExecutionListener<MainSessionSignal> {

	@Override
	protected MainSessionSignal signal(Instant start) {
		return createBatchSession(start);
	}
	
	@Override
	protected TraceUpdate update(TraceSignal signal) {
		return new MainSessionUpdate(signal.getId());
	}
}
