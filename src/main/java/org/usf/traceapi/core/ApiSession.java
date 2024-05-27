package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

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

	@Deprecated(forRemoval = true, since = "v22")
	private InstanceEnvironment application;
	private final Collection<ApiRequest> requests;
	private final Collection<DatabaseRequest> queries;
	private final Collection<RunnableStage> stages;
	//v22
	private String signature;

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
	
	static ApiSession synchronizedApiSession(String id) {
		var ss = new ApiSession(
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
		ss.setId(id);	
		return ss;
	}	
}