package org.usf.inspect.core;

import static java.util.Collections.emptyMap;

import java.util.Map;

/**
 * Provides application metadata for exported traces.
 */
public interface ApplicationPropertiesProvider {

	String getName();

	String getVersion();

	String getBranch();

	String getCommitHash();

	String getEnvironment();
	
	//v1.1
	/**
	 * Returns additional application properties to include in traces.
	 *
	 * @return the additional application properties
	 */
	default Map<String, String> additionalProperties() {
		return emptyMap(); 
	}
}
