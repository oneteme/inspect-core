package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a locally observed request signal with identifying metadata about the request source.
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

	/**
	 * Creates a local request signal with the supplied request identity and timing information.
	 *
	 * @param id the unique request identifier
	 * @param sessionId the identifier of the session that owns the request
	 * @param start the instant at which the request started
	 * @param threadName the name of the thread associated with the request
	 */
	public LocalRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	/**
	 * Creates an update callback object for this local request signal.
	 *
	 * @return a request update initialized with this signal identifier
	 */
	public LocalRequestUpdate createCallback() {
		return new LocalRequestUpdate(getId());
	}
}
