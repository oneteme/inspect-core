package org.usf.inspect.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;


/**
 * 
 * @author u$f
 *
 */
public interface HttpUserProvider {
	
	default String getUser(javax.servlet.http.HttpServletRequest req, String apiName) {
    	return ofNullable(req.getUserPrincipal())
    			.map(Principal::getName)
    			.orElse(null);
	}
}
