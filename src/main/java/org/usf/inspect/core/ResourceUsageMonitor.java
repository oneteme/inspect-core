package org.usf.inspect.core;

import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.time.Instant.now;

import java.lang.management.MemoryUsage;
import java.util.function.ToLongFunction;

import org.usf.inspect.core.Dispatcher.DispatchHook;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class ResourceUsageMonitor implements DispatchHook {

	private static final int MB = 1024 * 1024;
	
	@Override
	public void onInstanceEmit(Dispatcher dispatcher, InstanceEnvironment instance) {
		instance.setResource(getMemoryUsage(MemoryUsage::getInit, MemoryUsage::getMax));
	}

	@Override
	public void preDispatch(Dispatcher dispatcher) {
		dispatcher.emit(getMemoryUsage(MemoryUsage::getUsed, MemoryUsage::getCommitted));
	}
	
	static MachineResourceUsage getMemoryUsage(ToLongFunction<MemoryUsage> lowFn, ToLongFunction<MemoryUsage> hghFn) {
// 		var threadMXBean = ManagementFactory.getThreadMXBean()
		var memr = getMemoryMXBean();
		var heap = memr.getHeapMemoryUsage();
		var meta = memr.getNonHeapMemoryUsage();
    	return new MachineResourceUsage(now(), 
    			toMb(lowFn.applyAsLong(heap)), 
    			toMb(hghFn.applyAsLong(heap)), 
    			toMb(lowFn.applyAsLong(meta)), 
    			toMb(hghFn.applyAsLong(meta)));
	}
    
    static int toMb(long value) {
    	return (int) (value / MB);
    }
}
