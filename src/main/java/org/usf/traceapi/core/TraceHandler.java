package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface TraceHandler {
	
	void handle(Session session);

}
