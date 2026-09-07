package org.usf.inspect.core;

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
public class AbstractRequestUpdate implements TraceUpdate {

	private final UUID id;
	private String command; //READ, EMIT, EDIT, ..
	private Instant end;
	//v1.2 : replace failed property
	private short status = -1; //-1 unknown
	
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
