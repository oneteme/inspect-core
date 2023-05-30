package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class IncomingRequest extends OutcomingRequest {

	private String contentType;
	private String application;
	private String endpoint;
	private String resource;
	private String principal;
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