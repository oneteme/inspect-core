package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;

import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public class ResourceUsageMonitor implements DispatchListener {

//	private static ThreadMXBean threadMXBean = getThreadMXBean()
    
	private static final int MB = 1024 * 1024;
	
	@Override
	public void onDispatchEvent(DispatchState state, boolean complete) throws Exception {
		var memoryBean = getMemoryMXBean();
		var heapUsage = memoryBean.getHeapMemoryUsage();
		var metaUsage = memoryBean.getNonHeapMemoryUsage();
		var usedHeap = usedMemory(heapUsage);
    	var cmttHeap = committedMemory(heapUsage);
    	var usedMeta = usedMemory(metaUsage);
    	var cmttMeta = committedMemory(metaUsage);
    	InspectContext.emit(new ResourceUsage(now(), usedHeap, cmttHeap, usedMeta, cmttMeta));
	}
	
	public ResourceUsage getConfig() {
		var memoryBean = getMemoryMXBean();
		var heapUsage = memoryBean.getHeapMemoryUsage();
		var metaUsage = memoryBean.getNonHeapMemoryUsage();
		var intHeap = initHeapMemory(heapUsage);
    	var maxHeap = maxHeapMemory(heapUsage);
    	var intMeta = initHeapMemory(metaUsage);
    	var maxMeta = maxHeapMemory(metaUsage);
    	return new ResourceUsage(now(), intHeap, maxHeap, intMeta, maxMeta);
	}
    
    static int initHeapMemory(MemoryUsage mem) {
    	return (int) (mem.getInit() / MB);
    }

    static int maxHeapMemory(MemoryUsage mem) {
    	return (int) (mem.getMax() / MB);
    }
    
    static int usedMemory(MemoryUsage mem) {
    	return (int) (mem.getUsed() / MB);
    }
    
    static int committedMemory(MemoryUsage mem) {
    	return (int) (mem.getCommitted() / MB);
    }
}
