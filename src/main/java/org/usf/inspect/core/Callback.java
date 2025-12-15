package org.usf.inspect.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Callback extends CompletableTrace {
	
	Instant getEnd();
	
	void setEnd(Instant end);
}
