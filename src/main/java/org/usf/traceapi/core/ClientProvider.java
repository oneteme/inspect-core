package org.usf.traceapi.core;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface ClientProvider {
	
	String supply(HttpServletRequest req);
	
}