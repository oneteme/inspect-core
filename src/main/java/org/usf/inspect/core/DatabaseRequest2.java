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
public class DatabaseRequest2 extends AbstractRequest2 {

	private String scheme;
	private String host; //IP, domaine
	private int port; //-1 otherwise
	private String name; //nullable
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	
	public DatabaseRequest2(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	public DatabaseRequestCallback createCallback() {
		return new DatabaseRequestCallback(getId());
	}
}
