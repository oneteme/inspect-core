package org.usf.inspect.core;

import static org.usf.inspect.core.MainSessionType.STARTUP;

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
	//v1.2
	private UUID parentId;

	public MainSessionSignal(UUID id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type = type;
	}

	@Deprecated(forRemoval = false, since = "1.2")
	public MainSessionUpdate createCallback() {
		return new MainSessionUpdate(getId(), STARTUP.name().equals(type));
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
