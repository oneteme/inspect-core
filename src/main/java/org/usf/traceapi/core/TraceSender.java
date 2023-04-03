package org.usf.traceapi.core;

@FunctionalInterface
public interface TraceSender {
	
	void send(MainRequest mr);

}
