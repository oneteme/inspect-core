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
@JsonTypeName("main")
@JsonIgnoreProperties("lock")
public final class MainSession extends RunnableStage implements Session {
	
	private String id;
	private LaunchMode launchMode;
	private ApplicationInfo application;
	private final Collection<ApiRequest> requests;
	private final Collection<DatabaseRequest> queries;
	private final Collection<RunnableStage> stages;
	//name : @annotation, methodName, viewTitle, ..
	//location : URL, File, SI, ...

	private final AtomicInteger lock = new AtomicInteger();

	public MainSession() {
		this(new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
	}

	@JsonCreator
	public MainSession(Collection<ApiRequest> requests, Collection<DatabaseRequest> queries, Collection<RunnableStage> stages) {
		this.requests = requests;
		this.queries = queries; 
		this.stages = stages; 
	}

	@Override
	public void append(ApiRequest request) {
		requests.add(request);
	}

	@Override
	public void append(DatabaseRequest request) {
		queries.add(request);
	}
	
	@Override
	public void append(RunnableStage stage) {
		stages.add(stage);
	}
	
	static MainSession synchronizedMainSession(String id) {
		var ss = new MainSession(
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
		ss.setId(id);	
		return ss;
	}
	
}
