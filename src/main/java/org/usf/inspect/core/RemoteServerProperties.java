package org.usf.inspect.core;

import java.time.Duration;

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
	
	Duration getRetentionMaxAge();
	
	void validate();
}
