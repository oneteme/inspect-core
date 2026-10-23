package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@SuppressWarnings("serial")
public class TraceDispatchException extends Exception {

	public TraceDispatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public TraceDispatchException(String message) {
		super(message);
	}	
}
