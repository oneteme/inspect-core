package org.usf.inspect.core;

import static java.lang.String.join;
import static java.util.Objects.nonNull;

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
public final class RestClientProperties {
	
	private static final String HOST_PATTERN = "https?://[\\w\\-\\.]+(:\\d{2,5})?\\/?";
	private static final String PATH_PATTERN = "[\\w\\-\\{\\}]+(\\/[\\w\\-\\{\\}]+)*(\\?.+)?";
	private static final String SLASH = "/";
	
	private String host = "http://localhost:9000";
	private String instanceApi = "v3/trace/instance"; //[POST] async
	private String sessionApi  = "v4/trace/instance/{id}/session?pending={pending}&attemps={attemps}&end={end}"; //[PUT] async
	private int compressMinSize = 0; //in bytes, 0 no compress
	private int lazyAfter = 60; //in seconds > 0 
	
	void validate() {
		assertMatches(host, HOST_PATTERN);
		this.sessionApi  = join(SLASH, host, assertMatches(sessionApi, PATH_PATTERN));
		this.instanceApi = join(SLASH, host, assertMatches(instanceApi, PATH_PATTERN));
	}
	
	static String assertMatches(String s, String pattern){
		if(nonNull(s) && s.matches(pattern)) {
			return s;
		}
		throw new IllegalArgumentException("bad value=" + s + ", pattern=" + pattern);
	}
}
