package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;
import static org.usf.inspect.core.InspectContext.context;

import java.io.File;
import java.lang.management.MemoryMXBean;

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
	public void preDispatch() {
		var heap = bean.getHeapMemoryUsage();
		var meta = bean.getNonHeapMemoryUsage();
		context().emitTrace(new MachineResourceUsage(now(),
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
