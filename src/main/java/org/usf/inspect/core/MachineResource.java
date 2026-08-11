package org.usf.inspect.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes the machine resource limits detected for an inspected runtime environment.
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public final class MachineResource {
	
	private final int minHeap;
	private final int maxHeap;
//	private final int minMeta
//	private final int maxMeta
	private final int diskTotalSpace;
	//threads, CPU, .. ?
}
