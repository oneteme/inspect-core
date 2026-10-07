package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public abstract class AbstractSessionUpdate implements TraceUpdate {

	private final UUID id;
	private Instant end;
	private Instant start; //real start time
	private String name; //scheduler name, API name, view title, etc
	private String user;
	private String location; //class.method, URL, endpoint
	@Deprecated(forRemoval = true, since = "1.2")
	private ExceptionTrace exception; //trace exception separately
	private int eventMask; //see SessionEventMask
	//v1.2
	private short status; //see DualEventTracer
	private Long asyncDuration; //duration of async tasks, in ms
	
	public void setEnd(Instant end){
		this.end = end;
	}
	
	@JsonIgnore
	public boolean isStartup() {
		return false;
	}
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(end)
//				.withAction(command)
				.withMessageAsTopic(id.toString())
				.withStatus(getStatus()+"")
				.format();
	}
}
