package org.usf.inspect.core;

import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;
import static org.usf.inspect.core.InspectContext.context;

import java.time.Instant;

import org.usf.inspect.core.SafeCallable.SafeRunnable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionMonitor {
	
	public static <E extends Throwable> void exec(SafeRunnable<E> fn, ExecutionMonitorListener<? super Void> listener) throws E {
		call(fn, listener);
	}

	public static <T, E extends Throwable> T call(SafeCallable<T,E> fn, ExecutionMonitorListener<? super T> listener) throws E {
		T o = null;
		Throwable t = null;
		var s = now();
		try {
			return (o = fn.call());
		}
		catch(Throwable e) { //also error
			t = e;
			throw e;
		}
		finally {
			var e = now();
			EventTrace trace = null;
			try {
				trace = listener.handle(s, e, o, t);
			}
			catch (Throwable ex) {// do not throw exception
				reporter().action("ExecutionMonitor.call").cause(ex).emit();
			}
			finally {
				if(nonNull(trace)) {
					try {
						context().emitTrace(trace);
					}
					catch (Throwable ex) {// do not throw exception
						reporter().action("emitTrace").cause(ex).emit();
					}
				}
			}
		}
	}
	
	@FunctionalInterface
	public static interface ExecutionMonitorListener<T> {
		
		EventTrace handle(Instant start, Instant end, T o, Throwable t) throws Exception;
	}
}
