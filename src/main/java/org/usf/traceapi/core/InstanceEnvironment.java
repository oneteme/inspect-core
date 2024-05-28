package org.usf.traceapi.core;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;

/**
 * 
 * @author u$f
 *
 */
@Getter
@ToString
@RequiredArgsConstructor
public final class InstanceEnvironment {
	
	private final String name; //project name
	private final String version; //project version using : maven, NPM, ..
	@With
	private final String address; //IP address
	private final String env; //dev, rec, prod,..
	private final String os;  //operating system : Window, Linux,..
	private final String re;  //runtime environment : JAVA, Browsers,..
	//v22
	private final String user;
	private final InstantType type; //server, client
	private final Instant instant; //start time
	private final String collector; //spring-collector-xx, ng-collector-xx,..
	//commit, branch !?
}
