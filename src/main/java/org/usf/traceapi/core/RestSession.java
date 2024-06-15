package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;
import static org.usf.traceapi.core.Session.nextId;

import java.util.ArrayList;
import java.util.Collection;
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
public class RestSession extends RestRequest implements Session, MutableStage {

	private String name;
	@Deprecated(forRemoval = true, since = "v22")
	private InstanceEnvironment application;
	private Collection<RestRequest> requests;	
	private Collection<DatabaseRequest> queries;
	private Collection<RunnableStage> stages; //RunnableStage
	//v22
	private Collection<FtpRequest> ftpRequests;
	private Collection<MailRequest> mailRequests;
	private String userAgent; //Mozilla, Chrome, curl, Postman,..

	private final AtomicInteger lock = new AtomicInteger();
	
	public static RestSession synchronizedApiSession() {
		var ss = new RestSession();
		ss.setId(nextId());	
		ss.setRequests(synchronizedCollection(new ArrayList<>()));
		ss.setQueries(synchronizedCollection(new ArrayList<>()));
		ss.setFtpRequests(synchronizedCollection(new ArrayList<>()));
		ss.setStages(synchronizedCollection(new ArrayList<>()));
		return ss;
	}	
}