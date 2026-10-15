package org.usf.inspect;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface TraceUpdate extends TracePart {
	
	Instant getEnd();
	
	void setEnd(Instant end);
	
	short getStatus();

	void setStatus(short status);
	
	default void setStart(Instant end) {
		//do nothing 
	}
	
	byte traceType();
}
