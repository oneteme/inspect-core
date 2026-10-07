package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.contextPropagator;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContextPropagators {
	
    public static Runnable wrapRunnable(Runnable run) {
    	return wrapRunnable(run, true);
    }
	
    public static Runnable wrapRunnable(Runnable run, boolean updateThreadCount) {
    	var ctx = activeContext(); //do not use requireActiveContext
    	return isNull(ctx) ? run : ()-> runInContext(run, ctx, updateThreadCount);
	}
    
    public static void runInContext(Runnable run, SessionContext ctx) {
    	runInContext(run, ctx, true);
    }
    
    public static void runInContext(Runnable run, SessionContext ctx, boolean updateThreadCount) {
       	try(var sp = contextPropagator(ctx, updateThreadCount, "ContextPropagators.runInContext")){
       		run.run();
       	}
    }
    
    public static <T> Callable<T> wrapCallable(Callable<T> cmd) {
		return wrapCallable(cmd, true);
	}

    public static <T> Callable<T> wrapCallable(Callable<T> cmd, boolean updateThreadCount) {
    	var ctx = activeContext(); //do not use requireActiveContext
    	return isNull(ctx) ? cmd : ()-> callInContext(cmd, ctx, updateThreadCount);
	}

    public static <T> T callInContext(Callable<T> cmd, SessionContext ctx) throws Exception {
    	return callInContext(cmd, ctx, true);
    }
    
    public static <T> T callInContext(Callable<T> cmd, SessionContext ctx, boolean updateThreadCount) throws Exception {
       	try(var sp = contextPropagator(ctx, updateThreadCount, "ContextPropagators.callInContext")){
       		return cmd.call();
       	}
    }
    
    public static <T> Supplier<T> wrapSupplier(Supplier<T> cmd) {
    	return wrapSupplier(cmd, true);
    }
    
    public static <T> Supplier<T> wrapSupplier(Supplier<T> cmd, boolean updateThreadCount) {
    	var ctx = activeContext(); //do not use requireActiveContext
    	return isNull(ctx) ? cmd : ()-> supplyInContext(cmd, ctx, updateThreadCount);
	}
    
    public static <T> T supplyInContext(Supplier<T> cmd, SessionContext ctx) {
    	return supplyInContext(cmd, ctx, true);
    }
    
    public static <T> T supplyInContext(Supplier<T> cmd, SessionContext ctx, boolean updateThreadCount) {
       	try(var sp = contextPropagator(ctx, updateThreadCount, "ContextPropagators.supplyInContext")){
       		return cmd.get();
       	}
    }
}
