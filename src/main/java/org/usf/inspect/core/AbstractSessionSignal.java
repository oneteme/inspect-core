package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.formatLocation;

import java.time.Instant;
import java.util.UUID;

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
public class AbstractSessionSignal implements TraceSignal {

	private final UUID id;
	private final Instant start;
	private final String threadName;
	private String name;  //title, topic
	private String location; //class.method, URL
	private String user;

	//server usage 
	private UUID instanceId; 
	
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(start)
				.withThread(threadName)
				.withAction(name)
				.withUser(user)
				.withArgsAsTopic(location, null)
				.format();
	}
}
