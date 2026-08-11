package org.usf.inspect.core;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents the connection details and data retention policy for a remote collect server.
 *
 * @author u$f
 *
 */
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "@type")
public interface RemoteServerProperties {
	
	/**
	 * Returns the maximum age after which traces may be purged from the remote server.
	 *
	 * @return the trace retention duration
	 */
	Duration getRetentionMaxAge();
	
	/**
	 * Validates the configuration and resolves relative URI paths to absolute ones.
	 */
	void validate();
}
