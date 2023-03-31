package org.usf.traceapi.core;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface ClientSupplier {
	
	String clientId(HttpServletRequest req);
	
}