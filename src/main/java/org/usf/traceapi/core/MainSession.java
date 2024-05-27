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
	private String type; //@see 
	@Deprecated(forRemoval = true, since = "v22")
	private InstanceEnvironment application;
	private final Collection<ApiRequest> requests;
	private final Collection<DatabaseRequest> queries;
	private final Collection<RunnableStage> stages;

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
	
	@Deprecated(forRemoval = true, since = "v22")
	public String getLaunchMode() {
		return type;
	}

	@Deprecated(forRemoval = true, since = "v22")
	public void setLaunchMode(String type) {
		this.type = type;
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
