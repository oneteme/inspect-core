package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SessionHandler {
	
	void handle(Session session);
}