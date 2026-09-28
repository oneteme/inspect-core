package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.activeContext;
import static org.usf.inspect.core.SessionContextManager.clearContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface SessionPropagator extends AutoCloseable {
	
	@Override
	void close();
	
    static Runnable wrapRunnable(Runnable run) {
    	return wrapRunnable(run, true);
    }
	
    static Runnable wrapRunnable(Runnable run, boolean updateThreadCount) {
    	var ses = activeContext(); //do not use requireActiveContext
    	return isNull(ses) || ses.wasCompleted() ? run : ()-> runInContext(run, ses, updateThreadCount);
	}
    
    static void runInContext(Runnable run, AbstractSessionUpdate ctx) {
    	runInContext(run, ctx, true);
    }
    
    static void runInContext(Runnable run, AbstractSessionUpdate upd, boolean updateThreadCount) {
       	if(isNull(upd) || upd.wasCompleted()) {
    		run.run();
    		return;
    	}
       	try(var ctx = withContext(upd, updateThreadCount)){
       		run.run();
       	}
    }
    
    static <T> Callable<T> wrapCallable(Callable<T> cmd) {
		return wrapCallable(cmd, true);
	}

    static <T> Callable<T> wrapCallable(Callable<T> cmd, boolean updateThreadCount) {
    	var ses = activeContext(); //do not use requireActiveContext
    	return isNull(ses) || ses.wasCompleted() ? cmd : ()-> callInContext(cmd, ses, updateThreadCount);
	}

    static <T> T callInContext(Callable<T> cmd, AbstractSessionUpdate ctx) throws Exception {
    	return callInContext(cmd, ctx, true);
    }
    
    static <T> T callInContext(Callable<T> cmd, AbstractSessionUpdate upd, boolean updateThreadCount) throws Exception {
       	if(isNull(upd) || upd.wasCompleted()) {
    		return cmd.call();
    	}
       	try(var ctx = withContext(upd, updateThreadCount)){
       		return cmd.call();
       	}
    }
    
    static <T> Supplier<T> wrapSupplier(Supplier<T> cmd) {
    	return wrapSupplier(cmd, true);
    }
    
    static <T> Supplier<T> wrapSupplier(Supplier<T> cmd, boolean updateThreadCount) {
    	var ses = activeContext(); //do not use requireActiveContext
    	return isNull(ses) || ses.wasCompleted() ? cmd : ()-> supplyInContext(cmd, ses, updateThreadCount);
	}
    
    static <T> T supplyInContext(Supplier<T> cmd, AbstractSessionUpdate ctx) {
    	return supplyInContext(cmd, ctx, true);
    }
    
    static <T> T supplyInContext(Supplier<T> cmd, AbstractSessionUpdate upd, boolean updateThreadCount) {
       	if(isNull(upd) || upd.wasCompleted()) {
    		return cmd.get();
    	}
       	try(var ctx = withContext(upd, updateThreadCount)){
       		return cmd.get();
       	}
    }
    
    static SessionPropagator withContext(AbstractSessionUpdate ctx, boolean updateThreadCount) {
    	var prv = activeContext();
    	if(ctx != prv) {
			setActiveContext(ctx);
		}
    	if(updateThreadCount) {
    		ctx.threadCountUp();
    	}
    	return ()->{
    		if(prv != ctx) {
    			clearContext(ctx);
    			if(nonNull(prv)) {
    				setActiveContext(prv);
    			}
    		}
        	if(updateThreadCount) {
        		ctx.threadCountDown();
        	}
    	};
    }
}
