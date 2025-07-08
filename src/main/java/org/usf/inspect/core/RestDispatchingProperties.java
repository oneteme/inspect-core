package org.usf.inspect.core;

import static org.usf.inspect.core.Assertions.assertMatches;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ToString
public final class RestDispatchingProperties implements DispatchingProperties {
	
	private static final String INSTANCE_ENDPOINT = "v3/trace/instance"; //[POST] async
	private static final String SESSION_ENDPOINT  = "v4/trace/instance/{id}/session?pending={pending}&attemps={attemps}&end={end}"; //[PUT] async
	private static final String HOST_PATTERN = "https?://[\\w\\-\\.]+(:\\d{2,5})?\\/?";
	
	private String host = "http://localhost:9000";
	private int compressMinSize = 0; // size in bytes, 0: no compression
	
	@Override
	public void validate() {
		assertMatches(host, HOST_PATTERN, "host");
	}
	
	public String getInstanceEndpoint() {
		return INSTANCE_ENDPOINT;
	}
	
	public String getSessionEndpoint() {
		return SESSION_ENDPOINT;
	}
}
