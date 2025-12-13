package org.usf.inspect.core;

import java.time.Instant;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@Slf4j
@ToString
@RequiredArgsConstructor
public final class InstanceEnvironment {

	private final String id;
	private final Instant instant; //startup time TD : rename to start
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
	private MachineResource resource; //init/max heap +  init/max metaspace
	private Instant end;

}
