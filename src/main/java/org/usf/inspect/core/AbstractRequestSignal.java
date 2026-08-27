package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@RequiredArgsConstructor
public abstract class AbstractRequestSignal implements TraceSignal {

	private final UUID id;
	private final UUID sessionId;
	private final Instant start;
	private final String threadName;
	private String user;
	//server usage
	private UUID instanceId;
	
}
