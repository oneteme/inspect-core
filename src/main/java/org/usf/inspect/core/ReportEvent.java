package org.usf.inspect.core;

import static java.time.Clock.systemUTC;
import static org.usf.inspect.core.Helper.threadName;

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
@Setter
@Getter
@RequiredArgsConstructor
public final class ReportEvent implements EventTrace {
	
	private final Instant instant;
	private final String thread;
	private final String action;
	private final String message;
	private final StackTraceRow[] stackRows;
	@Deprecated(forRemoval = true, since = "v1.2")
	private String level; //type
	@Deprecated(forRemoval = true, since = "v1.2")
	private UUID sessionId; //optional
	
	//server usage 
	private UUID instanceId; 
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withInstant(instant)
		.withThread(thread)
		.withAction("REPORT")
		.withMessageAsTopic(action + " : " + message)
		.format();
	}
	
	static ReportEvent report(String action, String msg) {
		return report(action, msg, null);	
	}

	static ReportEvent report(String action, String msg, StackTraceRow[] stack) {
		return new ReportEvent(systemUTC().instant(), threadName(), action, msg, stack);	
	}
}