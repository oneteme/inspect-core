package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface TraceSender {
	
	void send(Metric metric);

}
