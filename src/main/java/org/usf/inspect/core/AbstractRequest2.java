package org.usf.inspect.core;

import java.time.Instant;

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
public abstract class AbstractRequest2 implements Initializer {

	private final String id;
	private final String sessionId;
	private final Instant start;
	private final String threadName;
	private String user;
	private String instanceId; //for distributed tracing
}
