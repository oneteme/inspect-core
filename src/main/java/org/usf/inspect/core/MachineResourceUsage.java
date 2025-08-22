package org.usf.inspect.core;

import static java.lang.String.format;

import java.time.Instant;

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
public final class MachineResourceUsage implements EventTrace {
	
	private final Instant instant;
	private final int usedHeap; 
	private final int commitedHeap;
	private final int usedMeta;
	private final int commitedMeta;
	private final int usedDiskSpace;
	private String instanceId; //server usage 
	//threads, CPU, disk ?
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withCommand("METRIC")
		.withInstant(instant)
		.withMessageAsTopic(format("heap: %d/%d | meta: %d/%d | disk:%d", 
				usedHeap, commitedHeap, usedMeta, commitedMeta, usedDiskSpace))
		.format();
	}
}
