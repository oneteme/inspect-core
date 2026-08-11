package org.usf.inspect.core;

import java.time.Instant;

/**
 * Defines mutable operations for atomically updating a trace.
 */
public interface AtomicTrace { //!Stateful
	
	/**
	 * Sets the real start time of the trace.
	 *
	 * @param start the start time to store
	 */
	void setStart(Instant start);
	
	/**
	 * Stores exception information on the trace.
	 *
	 * @param exception the exception information to store
	 */
	void setException(ExceptionInfo exception);
}
