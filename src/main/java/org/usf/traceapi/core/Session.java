package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends Metric {
	
	String getId(); //UUID
	
	String getName();
	
	String getUser();
	
	void append(OutcomingRequest request); // sub requests

	void append(OutcomingQuery query); // sub queries

}
