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
public abstract class AbstractSessionUpdate implements TraceUpdate, SessionTrace {

	private final UUID id;
	private Instant end;
	private Instant start; //real start time
	private String name; //scheduler name, API name, view title, etc
	private String user;
	private String location; //class.method, URL, endpoint
	@Deprecated(forRemoval = true, since = "1.2")
	private int requestMask; //see SessionRequestMask
	@Deprecated(forRemoval = true, since = "1.2")
	private ExceptionTrace exception; //trace exception separately
	private int eventMask; //see SessionEventMask
	//v1.2
	private short status; //see DualEventTracer
	private Long asyncDuration; //duration of async tasks, in ms
	
	public void setEnd(Instant end){
		this.end = end;
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
