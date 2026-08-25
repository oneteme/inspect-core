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
//	private final int usedMeta
//	private final int commitedMeta
	private final int usedDiskSpace;
	//1.2
	private final int activeThreadCount;
	private final int strartedThreadCount;
	private final int cpuUsage;

	//server usage 
	private String instanceId; 
	//CPU?
	
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
