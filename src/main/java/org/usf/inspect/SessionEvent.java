package org.usf.inspect;

import java.time.Instant;
import java.util.UUID;

import org.usf.inspect.core.EventTraceFormatter;

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
public final class SessionEvent implements EventTrace {

	private final Instant instant; //time of the event
	private final String type; //type of the event e.g. info|warn|error, click|scroll|resize, etc.
    private final String value; //value of the event e.g. message, event_name, resolution, etc.
    private final String scope;	//scope of the event e.g. threadname, etc.
    private final String location;  //location of the event e.g. dom, file, etc.
    private final UUID sessionId;
	//server usage
	private UUID instanceId;

    @Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction(type)
		.withMessageAsTopic(value + " : " + scope)
		.withInstant(instant)
		.format();
	}
}
