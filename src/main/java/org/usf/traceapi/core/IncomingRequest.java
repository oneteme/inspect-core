package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class IncomingRequest extends OutcomingRequest {

	private final String uuid;
	private String contentType;
	private String application;
	private String endpoint;
	private String resource;
	private String principal;
	private final Collection<OutcomingRequest> requests;
	private final Collection<OutcomingQuery> queries;
 	
	public IncomingRequest(String uuid, String url, String method, long start) {
		super(url, method, start);
		this.uuid = uuid;
		this.requests = synchronizedCollection(new LinkedList<>());
		this.queries = synchronizedCollection(new LinkedList<>());
	}
	
	public void push(OutcomingRequest request) {
		requests.add(request);
	}

	public void push(OutcomingQuery query) {
		queries.add(query);
	}
}