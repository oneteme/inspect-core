package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class MainRequest extends SubRequest {

	private final String uuid;
	private final String client;
	private final Collection<SubRequest> requests;
	private final Collection<MainQuery> queries;
 	
	public MainRequest(String uuid, String client, String url, String method, long start) {
		super(url, method, start);
		this.uuid = uuid;
		this.client = client;
		this.requests = synchronizedCollection(new LinkedList<>());
		this.queries = synchronizedCollection(new LinkedList<>());
	}
	
	public void push(SubRequest request) {
		requests.add(request);
	}

	public void push(MainQuery query) {
		queries.add(query);
	}
}