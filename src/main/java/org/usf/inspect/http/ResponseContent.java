package org.usf.inspect.http;

/**
 * Exposes captured HTTP response content metadata.
 */
public interface ResponseContent{
	
	/**
	 * Returns the captured response bytes when content caching is available.
	 *
	 * @return the captured response bytes, or {@code null} when not cached
	 */
	byte[] contentBytes();
	
	/**
	 * Returns the total size of the response content.
	 *
	 * @return the response content size in bytes
	 */
	long contentSize();
}