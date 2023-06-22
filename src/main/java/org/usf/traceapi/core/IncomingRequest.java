package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonCreator;

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
	private String endpoint; //nullable
	private String resource; //nullable
	private String client; 	 //nullable
	private String group;
	private final Collection<OutcomingRequest> requests;
	private final Collection<OutcomingQuery> queries;
	
	@JsonCreator //remove this
	public IncomingRequest(String id) {
		this(id, new LinkedList<>(), new LinkedList<>());
	}
	
	IncomingRequest(String id, Collection<OutcomingRequest> requests, Collection<OutcomingQuery> queries) {
		super(id);
		this.requests = requests;
		this.queries = queries; 
	}
	
	public void append(OutcomingRequest request) {
		requests.add(request);
	}

	public void append(OutcomingQuery query) {
		queries.add(query);
	}
	
	static IncomingRequest synchronizedIncomingRequest(String id) {
		return new IncomingRequest(id, 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
	}
	
}