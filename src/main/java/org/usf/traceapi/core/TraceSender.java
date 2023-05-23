package org.usf.traceapi.core;

@FunctionalInterface
public interface TraceSender {
	
	void send(IncomingRequest trc);

}
