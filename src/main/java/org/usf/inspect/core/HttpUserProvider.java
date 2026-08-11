package org.usf.inspect.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the user associated with an incoming HTTP request.
 * 
 * @author u$f
 *
 */
public interface HttpUserProvider {
	
	/**
	 * Returns the user name associated with the given HTTP request.
	 * 
	 * @param req the current HTTP servlet request
	 * @param apiName the logical API name associated with the request
	 * @return the resolved user name, or {@code null} when no principal is available
	 */
	default String getUser(HttpServletRequest req, String apiName) {
    	return ofNullable(req.getUserPrincipal())
    			.map(Principal::getName)
    			.orElse(null);
	}
}