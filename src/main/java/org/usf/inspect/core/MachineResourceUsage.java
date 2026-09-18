package org.usf.inspect.core;

import static java.lang.String.format;

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
public final class MachineResourceUsage implements EventTrace {
	
	private final Instant instant;

	private final int usedHeap; 
	private final int commitedHeap;
//	private final int usedMeta
//	private final int commitedMeta
	//using MB as int max value : 2^31-1  => 2.147 TB
	private final int usedDiskSpace;
	//1.2
	private final int activeThreadCount;
	private final int startedThreadCount;
	private final short cpuUsage; //avoid float precision issue, must be divided by 100 to get the real value, e.g. 1234 => 12.34%

	//server usage 
	private UUID instanceId; 
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withAction("METRIC")
		.withInstant(instant)
		.withMessageAsTopic(format("heap: %d/%d | disk:%d | threads:%d/%d | cpu:%d%%", 
				usedHeap, commitedHeap, usedDiskSpace, activeThreadCount, startedThreadCount, cpuUsage))
		.format();
	}
}
