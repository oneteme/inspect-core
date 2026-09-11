package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class SessionEvent implements EventTrace {

	private final Instant instant; //time of the event
	private final String type; //type of the event e.g. info|warn|error, click|scroll|resize, etc.
    private final String value; //value of the event e.g. message, resolution, etc.
    private final String location; //location of the event e.g. dom, file, etc.
    private final UUID sessionId;

    @Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(type)
		.withMessageAsTopic(value + " on " + location)
		.withInstant(instant)
		.format();
	}
}
