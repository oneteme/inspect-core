package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Marker interface for trace events serialized by the system.
 */
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.PROPERTY,
	    property = "@type")
public interface EventTrace { }