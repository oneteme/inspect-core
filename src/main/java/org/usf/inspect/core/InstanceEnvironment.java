package org.usf.inspect.core;

import java.time.Instant;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class InstanceEnvironment {

	private final Instant instant; //startup time
	private final InstanceType type; //server, client
	private final String name; //project name
	private final String version; //project version using : maven, NPM, ..
	private final String env; //dev, test, prod,..
	private final String address; //IP address
	private final String os;  //operating system : Window, Linux,..
	private final String re;  //runtime environment : JAVA, Browsers,..
	private final String user; //system user
	private final String branch; //branch name
	private final String hash; //commit hash$
	private final String collector; //spring-collector-xx, ng-collector-xx,..
	//v1.1
	private final Map<String, String> additionalProperties; //additional properties, e.g. for docker container, kubernetes pod, etc.
	private final InspectCollectorConfiguration configuration;
	
	private ResourceUsage resource; //init/max heap +  init/max metaspace
}
