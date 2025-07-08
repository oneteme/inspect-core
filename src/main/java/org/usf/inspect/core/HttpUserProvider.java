package org.usf.inspect.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
public interface HttpUserProvider {
	
	default String getUser(HttpServletRequest req, String apiName) {
    	return ofNullable(req.getUserPrincipal())
    			.map(Principal::getName)
    			.orElse(null);
	}
}