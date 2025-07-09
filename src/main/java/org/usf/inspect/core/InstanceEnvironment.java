package org.usf.inspect.core;

import static java.lang.System.getProperty;
import static java.net.InetAddress.getLocalHost;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.Helper.log;
import static org.usf.inspect.core.InstanceType.SERVER;

import java.net.UnknownHostException;
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
public class InstanceEnvironment {
	
	private final String name; //project name
	private final String version; //project version using : maven, NPM, ..
	private final String address; //IP address
	private final String env; //dev, test, prod,..
	private final String os;  //operating system : Window, Linux,..
	private final String re;  //runtime environment : JAVA, Browsers,..
	private final String user; //system user
	private final InstanceType type; //server, client
	private final Instant instant; //startup time
	private final String collector; //spring-collector-xx, ng-collector-xx,..
	private final String branch; //branch name
	private final String hash; //commit hash$
	//v1.1
	private final Map<String, String> additionalProperties; //docker labels, java system properties, etc.
	private InspectCollectorConfiguration configuration;
	
    public static InstanceEnvironment localInstance(Instant start, String name, String version, String branch, String hash, String env, Map<String, String> additionalProperties) {
    	return new InstanceEnvironment(name, version,
				hostAddress(),
				env,
				getProperty("os.name"), //version ? window 10 / Linux
				"java/" + getProperty("java.version"),
				getProperty("user.name"),
				SERVER,
				start,
				collectorID(),
				branch,
				hash,
				additionalProperties);
	}

	private static String hostAddress() {
		try {
			return getLocalHost().getHostAddress(); //hostName ?
		} catch (UnknownHostException e) {
			log.warn("error while getting host address", e);
			return null;
		}
	}
	
	private static String collectorID() {
		return "spring-collector/" //use getImplementationTitle
				+ requireNonNullElse(InstanceEnvironment.class.getPackage().getImplementationVersion(), "?");
	}
}
