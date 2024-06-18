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
@JsonTypeName("main")
@JsonIgnoreProperties("lock")
public class MainSession extends RunnableStage implements Session {
	
	private String id;
	private String type; //@see MainSessionType
	@Deprecated(forRemoval = true, since = "v22")
	private InstanceEnvironment application;
	private Collection<RestRequest> requests;
	private Collection<DatabaseRequest> queries;
	private Collection<RunnableStage> stages;
	//v22
	private Collection<FtpRequest> ftpRequests;
	private Collection<MailRequest> mailRequests;

	private final AtomicInteger lock = new AtomicInteger();
	
	@Deprecated(forRemoval = true, since = "v22")
	public String getLaunchMode() {
		return type;
	}

	@Deprecated(forRemoval = true, since = "v22")
	public void setLaunchMode(String type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return '['+type+']'+ super.toString();
	}
	
	public static MainSession synchronizedMainSession() {
		var ses = new MainSession();
		ses.setId(nextId());
		ses.setRequests(synchronizedCollection(new ArrayList<>()));
		ses.setQueries(synchronizedCollection(new ArrayList<>()));
		ses.setFtpRequests(synchronizedCollection(new ArrayList<>()));
		ses.setMailRequests(synchronizedCollection(new ArrayList<>()));
		ses.setStages(synchronizedCollection(new ArrayList<>()));
		return ses;
	}
}
