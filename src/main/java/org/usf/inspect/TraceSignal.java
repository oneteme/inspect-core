package org.usf.inspect;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface TraceSignal extends TracePart {

	Instant getStart();
}
