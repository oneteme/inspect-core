package org.usf.inspect.core;

import java.time.Instant;

/**
 * Represents a trace part that records a start time.
 * 
 * @author u$f
 *
 */
public interface TraceSignal extends TracePart {

	Instant getStart();
}
