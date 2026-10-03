package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.ReportEvent.report;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {
	
	InspectCollectorConfiguration getConfiguration();
	
	default void configure(InspectCollectorConfiguration config) { }

	default boolean canCollect() {return false;}

	default void dispatch(InstanceEnvironment instance) { }

	default void emitTrace(EventTrace trace) { }

	@Deprecated
	default boolean emitTraces(List<EventTrace> traces) {return false;}

	@Deprecated
	default boolean emitTask(DispatchTask task) {return false;}

	default void reportError(String action, Throwable thrw) {
		if(canCollect()) {
			if(isNull(thrw)) {
				thrw = new Exception(); //message will be null
			}
			var stck = getConfiguration().isDebugMode()
					? exceptionStackTraceRows(thrw, -1) 
					: null;
			emitTrace(report(action, thrw.getMessage(), stck));
		}
	}

	default void reportMessage(String action, String msg) {
		if(canCollect()) {
			var stck = getConfiguration().isDebugMode()
					? exceptionStackTraceRows(new Exception(), -1) 
					: null;
			emitTrace(report(action, msg, stck)); 
		}
	}

	default void emitExceptionTrace(Throwable thrw, TraceUpdate upd, long offset) {
		if(canCollect()) {
			var cfg = getConfiguration().getMonitoring().getException();
			var trc = upd instanceof AbstractSessionUpdate 
					? fromException(thrw, cfg.getMaxCauseDepth(), cfg.getMaxStackTraceRows())
					: fromException(thrw, 0, 0);
			if(nonNull(upd)) {
				trc.setTraceId(upd.getId());
				trc.setTraceType(upd.traceType());
			}
			trc.setOffset(offset);
			emitTrace(trc);
		}
	}

	default boolean isEnabled() {
		return nonNull(getConfiguration()) && getConfiguration().isEnabled();
	}
	
	static TraceHub hub() {
		return Holder.INSTANCE;
	}

	final class Holder {
		static TraceHub INSTANCE = new TraceDispatcherHub();
		private Holder() {}
	}
	
}