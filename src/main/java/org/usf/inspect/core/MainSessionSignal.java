package org.usf.inspect.core;

import static org.usf.inspect.core.MainSessionType.STARTUP;

import java.time.Instant;

import lombok.Getter;

/**
 * Represents the signal emitted when a main session begins.
 *
 * @author u$f 
 *
 */
@Getter
public final class MainSessionSignal extends AbstractSessionSignal {

	private final String type;

	/**
	 * Creates a main session signal with the supplied session metadata.
	 *
	 * @param id the session identifier
	 * @param start the session start time
	 * @param threadName the thread that opened the session
	 * @param type the session type name
	 */
	public MainSessionSignal(String id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type= type;
	}

	/**
	 * Creates the update object that tracks the completion of this main session.
	 *
	 * @return the update associated with this session signal
	 */
	public MainSessionUpdate createCallback() {
		return new MainSessionUpdate(getId(), STARTUP.name().equals(type));
	}
}
