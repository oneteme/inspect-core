package org.usf.traceapi.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface ApiUserProvider {
	
	String getUser(HttpServletRequest req);
	
}