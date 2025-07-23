package org.usf.inspect.core;

import static java.util.Collections.emptyMap;

import java.util.Map;

/**
 * 
 * @author u$f
 *
 */
public interface ApplicationPropertiesProvider {

	String getName();
	
	String getVersion();
	
	String getBranch();
	
	String getCommitHash();
	
	String getEnvironment();
	
	//v1.1
	default Map<String, String> additionalProperties() {
		return emptyMap(); 
	}
}
