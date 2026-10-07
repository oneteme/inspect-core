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
public final class MainSessionSignal extends AbstractSessionSignal {

	private final String type;
	
	public MainSessionSignal(UUID id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type = type;
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withAction(getType())
				.withUser(getUser())
				.withArgsAsTopic(getLocation(), null)
				.format();
	}
}
