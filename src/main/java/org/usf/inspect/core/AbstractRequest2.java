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
public abstract class AbstractRequest2 implements EventTrace {

	private final String id;
	private final String sessionId;
	private final Instant start;
	private final String threadName;
	private String user;
}
