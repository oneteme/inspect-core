package org.usf.inspect.http;

/**
 * 
 * @author u$f
 *
 */
public interface ResponseContent{
	
	byte[] contentBytes();
	
	long contentSize();
}