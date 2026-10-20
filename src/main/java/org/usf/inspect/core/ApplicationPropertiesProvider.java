package org.usf.inspect.core;

import static java.util.Collections.emptyMap;

import java.util.Map;

import jakarta.servlet.ServletContext;

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
	
	default Map<String, String> additionalProperties(ServletContext servletContext) {
		return emptyMap(); 
	}
}
