package org.usf.traceapi.core;

import static java.lang.String.join;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfigurationProperties extends SessionDispatcherProperties {
	
	private static final String SLASH = "/";
	
	private String host = "localhost:9000";
	private String instanceApi = "v3/trace/instance"; //[POST]
	private String sessionApi  = "v3/trace/instance/${id}/session"; //[PUT]
	
	public String instanceApiURL() {
		return toURL(host, instanceApi);
	}
	
	public String sessionApiURL() {
		return toURL(host, sessionApi);
	}
	
	private static String toURL(String host, String path) {
		return host.endsWith(SLASH) || path.startsWith(SLASH) //BOTH ?
				? host + path
				: join(SLASH, host, path);
	}
}
