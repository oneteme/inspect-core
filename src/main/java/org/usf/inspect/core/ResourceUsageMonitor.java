package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;
import static org.usf.inspect.core.InspectContext.emit;

import java.lang.management.MemoryUsage;
import java.util.function.ToLongFunction;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class ResourceUsageMonitor implements DispatchListener {

	private static final int MB = 1024 * 1024;
	
	@Override
	public void onDispatchEvent(DispatchState state, boolean complete) throws Exception {
    	emit(getMemory(MemoryUsage::getUsed, MemoryUsage::getCommitted));
	}
	
	public ResourceUsage startupResource() {
    	return getMemory(MemoryUsage::getInit, MemoryUsage::getMax);
	}
	
	static ResourceUsage getMemory(ToLongFunction<MemoryUsage> lowFn, ToLongFunction<MemoryUsage> hghFn) {
		var memoryBean = getMemoryMXBean();
// 		var threadMXBean = getThreadMXBean()
		var heapUsage = memoryBean.getHeapMemoryUsage();
		var metaUsage = memoryBean.getNonHeapMemoryUsage();
    	return new ResourceUsage(now(), 
    			toMb(lowFn.applyAsLong(heapUsage)), 
    			toMb(hghFn.applyAsLong(heapUsage)), 
    			toMb(lowFn.applyAsLong(metaUsage)), 
    			toMb(hghFn.applyAsLong(metaUsage)));
	}
    
    static int toMb(long value) {
    	return (int) (value / MB);
    }
}
