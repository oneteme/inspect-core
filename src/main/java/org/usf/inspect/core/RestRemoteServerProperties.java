package org.usf.inspect.core;

import static org.usf.inspect.core.Assertions.assertMatches;
import static org.usf.inspect.core.Assertions.assertStrictPositive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties({"ins", "ses"})
public final class RestRemoteServerProperties implements RemoteServerProperties {
	
	private static final String INSTANCE_ENDPOINT = "v3/trace/instance"; //[POST] async
	private static final String SESSION_ENDPOINT  = "v4/trace/instance/{id}/session?pending={pending}&attemps={attemps}&end={end}"; //[PUT] async
	private static final String HOST_PATTERN = "https?://[\\w\\-\\.]+(:\\d{2,5})?\\/?";
	
	private String ins;
	private String ses;
	
	private String host = "http://localhost:9000";
	private int retentionMaxAge = 30; //
	private int compressMinSize = 0; // size in bytes, 0: no compression
	
	@Override
	public void validate() {
		assertMatches(host, HOST_PATTERN, "host");
		if(!host.endsWith("/")) {
			host += "/"; //remove trailing slash
		}
		ins = host + INSTANCE_ENDPOINT;
		ses = host + SESSION_ENDPOINT;
		assertStrictPositive(retentionMaxAge, "retentionMaxAge");
	}
	
	public String getInstanceEndpoint() {
		return ins;
	}
	
	public String getSessionEndpoint() {
		return ses;
	}
}
