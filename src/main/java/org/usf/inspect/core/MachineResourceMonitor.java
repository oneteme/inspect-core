package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;

import java.io.File;
import java.lang.management.MemoryMXBean;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class MachineResourceMonitor implements DispatchHook {
	
	private static final int MB = 1024 * 1024;

	private final MemoryMXBean bean = getMemoryMXBean();
	private final File file; 

	@Override
	public void onInstanceEmit(InstanceEnvironment instance) {
		var heap = bean.getHeapMemoryUsage();
		var meta = bean.getNonHeapMemoryUsage();
		instance.setResource(new MachineResource(
				toMb(heap.getInit()), 
				toMb(heap.getMax()), 
				toMb(meta.getInit()), 
				toMb(meta.getMax()),
				toMb(file.getTotalSpace())));
	}

	@Override
	public void onDispatch(boolean complete, List<EventTrace> traces) {
		var heap = bean.getHeapMemoryUsage();
		var meta = bean.getNonHeapMemoryUsage();
		traces.add(new MachineResourceUsage(now(), //silent add trace !emit
				toMb(heap.getUsed()), 
				toMb(heap.getCommitted()), 
				toMb(meta.getUsed()), 
				toMb(meta.getCommitted()),
				toMb(file.getTotalSpace() - file.getUsableSpace()))); // used space
	}

	static int toMb(long value) {
		return (int) (value / MB);
	}
}
