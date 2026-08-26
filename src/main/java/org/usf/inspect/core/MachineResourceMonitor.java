package org.usf.inspect.core;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;

import java.io.File;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.function.IntSupplier;

/**
 * 
 * @author u$f
 *
 */
public final class MachineResourceMonitor implements DispatchHook {

	private static final int MB = 1024 * 1024;

	private final MemoryMXBean memoryBean = getMemoryMXBean();
	private final ThreadMXBean threadBean = getThreadMXBean();
	private final OperatingSystemMXBean osBean = getOperatingSystemMXBean();
	private final File file;
	
	private final IntSupplier processCpuLoad;
	
	private long totalDiskSpace;

	public MachineResourceMonitor(File file) {
		this.file = file;
		try {
			this.totalDiskSpace = file.getTotalSpace();
		} catch (Exception e) {
			//do nothing
		}
		processCpuLoad = processCpuLoadSupplier(osBean);
	}
	
	@Override
	public void onInstanceEmit(InstanceEnvironment instance) {
		try{
			var heap = memoryBean.getHeapMemoryUsage();
//			var meta = bean.getNonHeapMemoryUsage()
			totalDiskSpace = file.getTotalSpace();
			instance.setResource(new MachineResource(
					toMb(heap.getInit()), 
					toMb(heap.getMax()), 
//					toMb(meta.getInit()), 
//					toMb(meta.getMax()),
					toMb(totalDiskSpace),
					getRuntime().availableProcessors()));
		}
		catch(Exception e) {
			//ignore
		}
	}

	@Override
	public void onSchedule(TraceHub ctx) {
		try{
			var heap = memoryBean.getHeapMemoryUsage();
			var startedThreadCount = threadBean.getTotalStartedThreadCount();
//			var meta = bean.getNonHeapMemoryUsage()
			ctx.emitTrace(new MachineResourceUsage(systemUTC().instant(),
					toMb(heap.getUsed()), 
					toMb(heap.getCommitted()), 
//					toMb(meta.getUsed()), 
//					toMb(meta.getCommitted()),
					totalDiskSpace > 0 ? toMb(totalDiskSpace - file.getUsableSpace()) : -1,
					threadBean.getThreadCount(),
					startedThreadCount > MAX_VALUE ? -1 : (int) startedThreadCount,
							processCpuLoad.getAsInt()));
		}
		catch(Exception e) {
			ctx.reportError(false, "MachineResourceMonitor.onSchedule", e);
		}
	}

	static int toMb(long value) {
		return value > 0 ? (int) (value / MB) : -1;
	}
	
	static IntSupplier processCpuLoadSupplier(OperatingSystemMXBean osBean) {
		Method method = null;
		try {
			Class<?> mxBeanClass = Class.forName("com.sun.management.OperatingSystemMXBean");
			if (mxBeanClass.isInstance(osBean)) {
	            try {
	                method = mxBeanClass.getMethod("getProcessCpuLoad");
	            } catch (NoSuchMethodException e1) {
	                try {
	                    method = mxBeanClass.getMethod("getCpuLoad");
	                } catch (NoSuchMethodException e2) {
	                    //do nothing
	                }
	            }
	        }
        } catch (ClassNotFoundException e) {
            //do nothing
        }
		if(nonNull(method) && method.getReturnType() == double.class) {
			var m = method;
			return ()->{
				try {
					var v = (double) m.invoke(osBean);
					if(v >= 0) {
						return (int) Math.round(v * 100.);
					}
				} catch (Exception e) { 
					//do nothing
				}
				return -1;
			};
		}
		return ()-> -1;
	}
}
