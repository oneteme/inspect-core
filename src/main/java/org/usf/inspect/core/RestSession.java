package org.usf.inspect.core;

import java.util.List;
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
@JsonTypeName("rest")
@JsonIgnoreProperties("lock")
public class RestSession extends RestRequest implements Session, MutableStage {

	private String name;
	private List<RestRequest> restRequests;	
	private List<DatabaseRequest> databaseRequests;
	private List<LocalRequest> localRequests; //RunnableStage
	private List<FtpRequest> ftpRequests;
	private List<MailRequest> mailRequests;
	private List<NamingRequest> ldapRequests;
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache
	//v1.0.2
	private List<Trace> traces;

	private final AtomicInteger lock = new AtomicInteger();
}