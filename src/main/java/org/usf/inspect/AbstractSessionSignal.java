package org.usf.inspect;

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
public class AbstractSessionSignal implements TraceSignal, SessionTrace  {

	private final UUID id;
	private final Instant start;
	private final String threadName;
	private String name;  //title, topic
	private String location; //class.method, URL
	private String user;
	//v1.2
	private int eventMask; //see SessionEventMask

	//server usage 
	private UUID instanceId;
}
