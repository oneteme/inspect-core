package org.usf.traceapi.core;

import static java.util.Collections.synchronizedList;
import static org.usf.traceapi.core.Session.nextId;

import java.util.ArrayList;
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
	private String type; //@see MainSessionType
	private List<RestRequest> restRequests;
	private List<DatabaseRequest> databaseRequests;
	private List<LocalRequest> localRequests;
	//v22
	private List<FtpRequest> ftpRequests;
	private List<MailRequest> mailRequests;
	private List<NamingRequest> ldapRequests;

	private final AtomicInteger lock = new AtomicInteger();
	
	@Override
	public String toString() {
		return '['+type+']'+ super.toString();
	}
	
	public static MainSession synchronizedMainSession() {
		var ses = new MainSession();
		ses.setId(nextId());
		ses.setRestRequests(synchronizedList(new ArrayList<>()));
		ses.setDatabaseRequests(synchronizedList(new ArrayList<>()));
		ses.setFtpRequests(synchronizedList(new ArrayList<>()));
		ses.setMailRequests(synchronizedList(new ArrayList<>()));
		ses.setLdapRequests(synchronizedList(new ArrayList<>()));
		ses.setLocalRequests(synchronizedList(new ArrayList<>()));
		return ses;
	}
}
