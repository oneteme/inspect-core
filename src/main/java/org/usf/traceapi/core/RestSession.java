package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

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
@JsonIgnoreProperties("lock")
public final class RestSession extends RestRequest implements Session { //IncomingRequest

	@Deprecated(forRemoval = true, since = "v22")
	private InstanceEnvironment application;
	private Collection<RestRequest> requests;	
	private Collection<DatabaseRequest> queries;
	private Collection<SessionStage> stages;
	//v22
	private Collection<FtpRequest> ftpRequests;
	private String signature;

	private final AtomicInteger lock = new AtomicInteger();
	
	public static RestSession synchronizedApiSession(String id) {
		var ss = new RestSession();
		ss.setId(id);	
		ss.setRequests(synchronizedCollection(new LinkedList<>()));
		ss.setQueries(synchronizedCollection(new LinkedList<>()));
		ss.setFtpRequests(synchronizedCollection(new LinkedList<>()));
		ss.setStages(synchronizedCollection(new LinkedList<>()));
		return ss;
	}	
}