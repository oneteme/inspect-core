package org.usf.inspect.core;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.lang.management.ManagementFactory.getOperatingSystemMXBean;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.time.Clock.systemUTC;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.TraceHub.hub;

import java.io.File;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.function.IntSupplier;

import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.MachineResource;
import org.usf.inspect.SystemMetrics;
import org.usf.inspect.config.SystemMonitoringProperties;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class SystemMonitor implements DispatchHook {

	private static final int MB = 1024 * 1024;

	private static final MemoryMXBean memoryBean = getMemoryMXBean();
	private static final ThreadMXBean threadBean = getThreadMXBean();
	private static final OperatingSystemMXBean osBean = getOperatingSystemMXBean();
	
	private final File file;
	private final IntSupplier processCpuLoad;
	
	private long totalDiskSpace;

	public SystemMonitor(SystemMonitoringProperties prop) {
		this.file = prop.getDisk();
		this.processCpuLoad = processCpuLoadSupplier(osBean);
	}
	
	@Override
	public void onInstanceEmit(InstanceEnvironment instance) {
		var heap = memoryBean.getHeapMemoryUsage();
//		var meta = bean.getNonHeapMemoryUsage()
		totalDiskSpace = file.getTotalSpace();
		instance.setResource(new MachineResource(
				toMb(heap.getInit()), 
				toMb(heap.getMax()), 
//					toMb(meta.getInit()), 
//					toMb(meta.getMax()),
				toMb(totalDiskSpace),
				getRuntime().availableProcessors()));
	}

	@Override
	public void onSchedule() {
		var heap = memoryBean.getHeapMemoryUsage();
		var startedThreadCount = threadBean.getTotalStartedThreadCount();
//			var meta = bean.getNonHeapMemoryUsage()
		hub().emitTrace(new SystemMetrics(systemUTC().instant(),
				toMb(heap.getUsed()), 
				toMb(heap.getCommitted()), 
//					toMb(meta.getUsed()), 
//					toMb(meta.getCommitted()),
				totalDiskSpace > 0 ? toMb(totalDiskSpace - file.getUsableSpace()) : -1,
				threadBean.getThreadCount(),
				startedThreadCount > MAX_VALUE ? -1 : (int) startedThreadCount, (short)processCpuLoad.getAsInt()));
	}

	static int toMb(long value) {
		return value >= 0 ? (int) (value / MB) : -1;
	}
	
	static IntSupplier processCpuLoadSupplier(OperatingSystemMXBean osBean) {
		Method method = null;
		try {
			var mxBeanClass = Class.forName("com.sun.management.OperatingSystemMXBean");
			if (mxBeanClass.isInstance(osBean)) {
				for(var name : new String[] {"getProcessCpuLoad", "getCpuLoad"}) {
		            try {
		                method = mxBeanClass.getMethod(name);
		                break;
		            } catch (NoSuchMethodException e1) {
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
						return (int) Math.round(v * 10000);
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
