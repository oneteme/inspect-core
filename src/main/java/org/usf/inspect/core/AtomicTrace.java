package org.usf.inspect.core;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
public interface AtomicTrace { //!Stateful
	
	void setStart(Instant start); //real start 
	
	void setException(ExceptionInfo exception);
}
