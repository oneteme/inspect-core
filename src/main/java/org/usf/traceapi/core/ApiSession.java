package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@JsonTypeName("api")
public final class ApiSession extends ApiRequest implements Session {

	private ApplicationInfo application;
	private final Collection<ApiRequest> requests;
	private final Collection<DatabaseRequest> queries;
	private final Collection<RunnableStage> stages;
	
	private final AtomicInteger lock = new AtomicInteger();
	
	public ApiSession(String id) {
		this(id, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
	}
	
	@JsonCreator
	public ApiSession(String id, Collection<ApiRequest> requests, Collection<DatabaseRequest> queries, Collection<RunnableStage> stages) {
		super(id);
		this.requests = requests;
		this.queries = queries; 
		this.stages = stages; 
	}
	
	public void append(ApiRequest request) {
		requests.add(request);
	}

	public void append(DatabaseRequest query) {
		queries.add(query);
	}
	
	@Override
	public void append(RunnableStage stage) {
		stages.add(stage);
	}
	
	static ApiSession synchronizedIncomingRequest(String id) {
		return new ApiSession(id, 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
	}
	
}