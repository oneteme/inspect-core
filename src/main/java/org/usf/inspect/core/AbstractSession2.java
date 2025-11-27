package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.formatLocation;

import java.time.Instant;

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
public class AbstractSession2 implements Initializer {

	private final String id;
	private final Instant start;
	private final String threadName;
	private String name;  //title, topic
	private String location; //class.method, URL
	private String user;
	private String instanceId; //for distributed tracing
	
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}
}
