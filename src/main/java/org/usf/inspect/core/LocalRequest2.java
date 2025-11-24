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
public class LocalRequest2 extends AbstractRequest2 {

	private String name; //title, topic
	private String type; //CONST, FILE, CACHE, .. 
	private String location; //class.method, URL

	public LocalRequest2(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public LocalRequestCallback createCallback() {
		return new LocalRequestCallback(getId());
	}
}
