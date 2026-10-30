package org.usf.inspect;

import static java.time.Instant.ofEpochMilli;

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
public final class ExceptionTrace implements EventTrace {
	
	private final String type; //className
	private final String message;
	private final StackTraceRow[] stackTraceRows; //optional, can be null
	private final ExceptionTrace cause; //optional, can be null
	//v1.2
	private byte traceType; //TraceType
	private long offset; //order | duration
	private UUID traceId; //request | session
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(traceType < 10 ? ofEpochMilli(offset) : null)
//				.withAction(command)
				.withAction(type)
				.withMessageAsTopic(message)
				.format();
	}
}
