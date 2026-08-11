package org.usf.inspect.core;

/**
 * Runtime exception thrown when trace dispatch fails.
 */
@SuppressWarnings("serial")
public final class DispatchException extends RuntimeException {

	/**
	 * Creates a dispatch exception with a message and root cause.
	 *
	 * @param message the exception message
	 * @param cause the root cause
	 */
	public DispatchException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a dispatch exception with a message.
	 *
	 * @param message the exception message
	 */
	public DispatchException(String message) {
		super(message);
	}
}
