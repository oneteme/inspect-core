package org.usf.inspect.core;

import static java.lang.String.format;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents a point-in-time snapshot of machine resource consumption emitted as an event trace.
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@RequiredArgsConstructor
public final class MachineResourceUsage implements EventTrace {
	
	private final Instant instant;
	private final int usedHeap; 
	private final int commitedHeap;
	private final int usedDiskSpace;
	private String instanceId; //server usage 
	//threads, CPU, disk ?
	
	/**
	 * Returns this resource usage snapshot as a formatted event trace string.
	 *
	 * @return the formatted string representation of this resource usage snapshot
	 */
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction("METRIC")
		.withInstant(instant)
		.withMessageAsTopic(format("heap: %d/%d | disk:%d", 
				usedHeap, commitedHeap, usedDiskSpace))
		.format();
	}
}
