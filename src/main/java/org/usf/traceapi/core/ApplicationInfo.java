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
public final class ApplicationInfo {
	
	private final String name;
	private final String version; // maven, NPM, ..
	@With
	private final String address; //IP address
	private final String env; //dev, rec, prod, ...
	private final String os;  //operating system : Window, Linux, ...
	private final String re;  //runtime environment : JAVA, Browsers, ...
	//v21
	private final Instant instant; //start time
	private final String collector; //spring-collector-xx, ng-collector-xx
	//commit,branch !?
}
