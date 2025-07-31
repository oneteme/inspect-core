package org.usf.inspect.core;

import static java.lang.String.format;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class MachineResourceUsage implements EventTrace {
	
	private final Instant instant;
	private final int usedHeap; 
	private final int commitedHeap;
	private final int usedMeta;
	private final int commitedMeta;
	private final int usedDiskSpace;
	//threads, CPU, disk ?
	
	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withCommand("METRIC")
		.withInstant(instant)
		.withMessageAsResource(format("heap: %d/%d | meta: %d/%d | disk:%d", usedHeap, commitedHeap, usedMeta, commitedMeta, usedDiskSpace))
		.format();
	}
}
