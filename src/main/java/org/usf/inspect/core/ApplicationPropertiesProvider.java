package org.usf.inspect.core;

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
	
	Map<String, String> additionalProperties();
}
