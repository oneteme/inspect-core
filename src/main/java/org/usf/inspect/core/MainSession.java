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
@JsonTypeName("main")
@JsonIgnoreProperties("lock")
public class MainSession extends LocalRequest implements Session {
	
	private String id;
//	inherits String type //@see MainSessionType
	private List<RestRequest> restRequests;
	private List<DatabaseRequest> databaseRequests;
	private List<LocalRequest> localRequests;
	private List<FtpRequest> ftpRequests;
	private List<MailRequest> mailRequests;
	private List<NamingRequest> ldapRequests;
	//v1.0.2
	private List<Trace> traces;

	private final AtomicInteger lock = new AtomicInteger();
}
