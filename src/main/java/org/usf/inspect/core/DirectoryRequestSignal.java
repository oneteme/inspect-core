package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DirectoryRequestSignal extends AbstractRemoteRequestSignal {
	
	public DirectoryRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public DirectoryRequestUpdate createCallback() {
		return new DirectoryRequestUpdate(getId());
	}
}
