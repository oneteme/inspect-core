package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
public final class MainRequest implements Session {
	
	private final String id;
	private String name; // methodName, viewTitle, ..
	private String user; //batch / webapp user
 	private Instant start;
	private Instant end;
	private LaunchMode launchMode;
	private String location; //URL, IP Address, SI, ... 
	private boolean failed;
	private String threadName;
	private String operatingSystem; //operating system : Window, Linux, ...
	private String runtimeEnvironment; //runtime environment : JAVA, JS, PHP, Browser, Postman ...
	private String projectEnvironment; //dev, rec, prod, ...
	private final Collection<OutcomingRequest> requests;
	private final Collection<OutcomingQuery> queries;

	@JsonCreator //remove this
	public MainRequest(String id) {
		this(id, new LinkedList<>(), new LinkedList<>());
	}
	public void append(OutcomingRequest request) {
		requests.add(request);
	}

	public void append(OutcomingQuery query) {
		queries.add(query);
	}
	
	static MainRequest synchronizedMainRequest(String id) {
		return new MainRequest(id, 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
	}
	
}
