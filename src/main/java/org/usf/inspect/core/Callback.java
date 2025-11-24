package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface Callback extends EventTrace {
	
	String getId();
	
	Instant getEnd();
	
	default boolean assertStillConnected(){
		if(nonNull(getEnd())) {
			reporter().action("assertStillConnected").trace(this).emit();
			return false;
		}
		return true;
	}
}
