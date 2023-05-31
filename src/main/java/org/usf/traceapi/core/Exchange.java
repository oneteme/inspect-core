package org.usf.traceapi.core;

import lombok.Getter;

/**
 * 
 * @author u$f
 *
 */
//TODO move to server side
@Getter
public final class Exchange extends OutcomingRequest {
	
	private IncomingRequest remoteTrace;

	public Exchange(String id) {
		super(id);
	}
	
}
