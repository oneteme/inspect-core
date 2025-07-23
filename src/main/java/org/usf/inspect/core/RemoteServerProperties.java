package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * @author u$f
 *
 */
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "@type")
public interface RemoteServerProperties {
	
	int getRetentionMaxAge();
	
	void validate();
}
