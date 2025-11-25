package org.usf.inspect.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Initializer extends EventTrace {

	String getId();
	
	Instant getStart();
}
