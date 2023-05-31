package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class IncomingRequest extends OutcomingRequest {

	private String contentType;
	private String application;
	private String endpoint; //nullable
	private String resource; //nullable
	private String client; 	 //nullable
	private String query;
	private final Collection<OutcomingRequest> requests;
	private final Collection<OutcomingQuery> queries;
 	
	public IncomingRequest(String id) {
		super(id);
		this.requests = synchronizedCollection(new LinkedList<>());
		this.queries = synchronizedCollection(new LinkedList<>());
	}
	
	public void append(OutcomingRequest request) {
		requests.add(request);
	}

	public void append(OutcomingQuery query) {
		queries.add(query);
	}
}