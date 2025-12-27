package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Clock.systemUTC;

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
		try{
			var heap = bean.getHeapMemoryUsage();
//			var meta = bean.getNonHeapMemoryUsage()
			instance.setResource(new MachineResource(
					toMb(heap.getInit()), 
					toMb(heap.getMax()), 
//					toMb(meta.getInit()), 
//					toMb(meta.getMax()),
					toMb(file.getTotalSpace())));
		}
		catch(Exception e) {
			//ignore
		}
	}

	@Override
	public void onSchedule(TraceHub ctx) {
		try{
			var heap = bean.getHeapMemoryUsage();
//			var meta = bean.getNonHeapMemoryUsage()
			ctx.emitTrace(new MachineResourceUsage(systemUTC().instant(),
					toMb(heap.getUsed()), 
					toMb(heap.getCommitted()), 
//					toMb(meta.getUsed()), 
//					toMb(meta.getCommitted()),
					toMb(file.getTotalSpace() - file.getUsableSpace()))); // used space
		}
		catch(Exception e) {
			ctx.reportError(false, "MachineResourceMonitor.onSchedule", e);
		}
	}

	static int toMb(long value) {
		return (int) (value / MB);
	}
}
