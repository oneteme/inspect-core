package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;
import static java.util.Objects.nonNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonTypeName("api")
@JsonIgnoreProperties({"location", "lock"})
public final class ApiSession extends ApiRequest implements Session { //IncomingRequest

	private ApplicationInfo application;
	private final Collection<ApiRequest> requests;
	private final Collection<DatabaseRequest> queries;
	private final Collection<RunnableStage> stages;
	
	private final AtomicInteger lock = new AtomicInteger();
	
	public ApiSession() {
		this(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
	}
	
	@JsonCreator
	public ApiSession(Collection<ApiRequest> requests, Collection<DatabaseRequest> queries, Collection<RunnableStage> stages) {
		this.requests = requests;
		this.queries = queries; 
		this.stages = stages; 
	}
	
	@Override
	public void setId(String id) {
		if(nonNull(getId())) {
			throw new IllegalStateException();
		}
		super.setId(id);
	}
	
	public void append(ApiRequest request) {
		requests.add(request);
	}

	public void append(DatabaseRequest request) {
		queries.add(request);
	}
	
	@Override
	public void append(RunnableStage stage) {
		stages.add(stage);
	}
	
	static ApiSession synchronizedApiSession(String id) {
		var ss = new ApiSession(
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
		ss.setId(id);	
		return ss;
	}
	
}