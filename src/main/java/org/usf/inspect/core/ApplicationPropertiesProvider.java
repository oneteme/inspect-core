package org.usf.inspect.core;

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
}
