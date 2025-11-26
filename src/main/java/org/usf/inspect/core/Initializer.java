package org.usf.inspect.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Initializer extends Compleatable {

	Instant getStart();
}
