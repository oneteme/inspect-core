package org.usf.inspect.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface TraceSignal extends TracePart {

	Instant getStart();
}
