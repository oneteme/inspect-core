package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Clock.systemUTC;

import java.io.File;
import java.lang.management.MemoryMXBean;

import lombok.RequiredArgsConstructor;

/**
 * Monitors heap and disk usage information and emits resource snapshots into the tracing pipeline.
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class MachineResourceMonitor implements DispatchHook {

	private static final int MB = 1024 * 1024;

	private final MemoryMXBean bean = getMemoryMXBean();
	private final File file; 

	/**
	 * Populates the instance environment with static machine resource capacity information.
	 *
	 * @param instance the instance environment to enrich with machine resource data
	 */
	@Override
	public void onInstanceEmit(InstanceEnvironment instance) {
		try{
			var heap = bean.getHeapMemoryUsage();
			instance.setResource(new MachineResource(
					toMb(heap.getInit()), 
					toMb(heap.getMax()), 
					toMb(file.getTotalSpace())));
		}
		catch(Exception e) {
			//ignore
		}
	}

	/**
	 * Emits the current machine resource usage as a trace event.
	 *
	 * @param ctx the trace hub that receives the generated resource usage trace
	 */
	@Override
	public void onSchedule(TraceHub ctx) {
		try{
			var heap = bean.getHeapMemoryUsage();
			ctx.emitTrace(new MachineResourceUsage(systemUTC().instant(),
					toMb(heap.getUsed()), 
					toMb(heap.getCommitted()), 
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
