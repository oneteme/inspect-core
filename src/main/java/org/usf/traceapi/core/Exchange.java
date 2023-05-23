package org.usf.traceapi.core;

import lombok.Getter;

//TODO move to server side
@Getter
public final class Exchange extends OutcomingRequest {
	
	private IncomingRequest remoteTrace;

	public Exchange(String id) {
		super(id);
	}
	
}
