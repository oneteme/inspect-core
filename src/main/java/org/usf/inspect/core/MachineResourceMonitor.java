package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class MachineResourceMonitor implements DispatchHook {

	private static final int MB = 1024 * 1024;
	private final File file; 

	@Override
	public void onInstanceEmit(InstanceEnvironment instance) {
		var memr = getMemoryMXBean();
		var heap = memr.getHeapMemoryUsage();
		var meta = memr.getNonHeapMemoryUsage();
		instance.setResource(new MachineResource(
				toMb(heap.getInit()), 
				toMb(heap.getMax()), 
				toMb(meta.getInit()), 
				toMb(meta.getMax()),
				toMb(file.getTotalSpace())));
	}

	@Override
	public void onDispatch(boolean complete, EventTrace[] traces) {
		var memr = getMemoryMXBean();
		var heap = memr.getHeapMemoryUsage();
		var meta = memr.getNonHeapMemoryUsage();
		context().emitTrace(new MachineResourceUsage(now(),
				toMb(heap.getUsed()), 
				toMb(heap.getCommitted()), 
				toMb(meta.getUsed()), 
				toMb(meta.getCommitted()),
				toMb(file.getTotalSpace() - file.getUsableSpace())));
	}

	static int toMb(long value) {
		return (int) (value / MB);
	}
}
