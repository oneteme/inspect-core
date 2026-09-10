package org.usf.inspect.http;

import static java.util.Objects.nonNull;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebUtils {
    
	public static final String TRACE_ID_HEADER = "Inspect-Trace-Id"; //X-Request-Id
	public static final String TRACE_RETRY_HEADER = "Inspect-Trace-Retry";

	public static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("\\w+ .+") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
}
