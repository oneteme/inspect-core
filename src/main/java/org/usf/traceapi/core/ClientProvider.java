package org.usf.traceapi.core;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface ClientProvider {
	
	String getClientId(HttpServletRequest req);
	
}