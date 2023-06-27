package org.usf.traceapi.core;

public interface Session extends Metric {
	
	String getId(); //UUID
	
	void append(OutcomingRequest request); // sub requests

	void append(OutcomingQuery query); // sub queries

}
