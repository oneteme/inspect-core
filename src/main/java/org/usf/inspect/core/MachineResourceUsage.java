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
	private final int lowHeap; //heap used or min
	private final int highHeap; //heap committed or max
	private final int lowMeta; //metaspace used or min 
	private final int highMeta; //metaspace committed or max 
	//threads, CPU, disk ?
	
	@Override
	public String toString() {
		return format("%s ~ heap: %d/%d, meta: %d/%d", instant, lowHeap, highHeap, lowMeta, highMeta);
	}
}
