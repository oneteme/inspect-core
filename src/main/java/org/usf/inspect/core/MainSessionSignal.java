package org.usf.inspect.core;

import static org.usf.inspect.core.MainSessionType.STARTUP;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

/**
 * 
 * @author u$f 
 *
 */
@Getter
public final class MainSessionSignal extends AbstractSessionSignal {

	private final String type;

	public MainSessionSignal(UUID id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type= type;
	}

	@Deprecated
	public MainSessionUpdate createCallback() {
		return new MainSessionUpdate(getId(), STARTUP.name().equals(type));
	}
}
