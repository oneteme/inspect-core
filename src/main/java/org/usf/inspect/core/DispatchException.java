package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@SuppressWarnings("serial")
public final class DispatchException extends Exception {

	public DispatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public DispatchException(String message) {
		super(message);
	}
}
