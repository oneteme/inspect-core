package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class LocalRequestSignal extends AbstractRequestSignal {

	private String name; //title, topic
	private String type; //CONST, FILE, CACHE, .. 
	private String location; //class.method, URL

	public LocalRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public LocalRequestUpdate createCallback() {
		return new LocalRequestUpdate(getId());
	}
}
