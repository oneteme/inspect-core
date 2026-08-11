package org.usf.inspect.core;

import java.time.Instant;

/**
 * Represents a trace part that records an end time update.
 * 
 * @author u$f
 *
 */
public interface TraceUpdate extends TracePart {

	Instant getEnd();

	void setEnd(Instant end);
}
