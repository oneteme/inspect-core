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
public final class MailRequestSignal extends AbstractRemoteRequestSignal {

	public MailRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

}
