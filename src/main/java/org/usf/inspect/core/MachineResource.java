package org.usf.inspect.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class MachineResource {
	
	private final int minHeap;
	private final int maxhHeap;
	private final int minMeta;
	private final int maxhMeta;
	private final int diskTotalSpace;
	//threads, CPU, .. ?
	
}
