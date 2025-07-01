package org.usf.inspect.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface HttpUserProvider {
	
	String getUser(HttpServletRequest req, String apiName);
    
    static String getUserPrincipal(HttpServletRequest req, String apiName){
    	return ofNullable(req.getUserPrincipal())
    			.map(Principal::getName)
    			.orElse(null);
    }
}