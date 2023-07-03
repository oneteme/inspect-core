package org.usf.traceapi.core;

import java.util.Collection;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends Metric {
	
	String getId(); //UUID
	
	String getName();
	
	String getUser();
	
	Collection<OutcomingRequest> getRequests();
	
	Collection<OutcomingQuery> getQueries();
	
	void append(OutcomingRequest request); // sub requests

	void append(OutcomingQuery query); // sub queries

}
