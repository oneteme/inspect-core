package org.usf.traceapi.core;

import lombok.Getter;

@Getter
public final class Exchange extends OutcomingRequest {
	
	private IncomingRequest remoteTrace;

	public Exchange(String id) {
		super(id);
	}
	
}
