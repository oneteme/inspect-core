package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.ReportEvent.report;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {
	
	InspectCollectorConfiguration getConfiguration();
	
	default void configure(InspectCollectorConfiguration config) { }

	default boolean canCollect() {return false;}

	default void dispatch(InstanceEnvironment instance) throws DispatchException { }

	default void emitTrace(EventTrace trace) { }

	@Deprecated
	default boolean emitTask(DispatchTask task) {return false;}

	default void emitReport(String action, Throwable thrw) {
		if(canCollect()) {
			var msg = nonNull(thrw) ? thrw.getClass().getName() + ":" + thrw.getMessage() : null;
			var stc = getConfiguration().isDebugMode()
					? exceptionStackTraceRows(nonNull(thrw) ? thrw : new Exception(), -1) 
					: null;
			emitTrace(report(action, msg, stc));
		}
	}

	default void emitReport(String action, String msg) {
		if(canCollect()) {
			var stc = getConfiguration().isDebugMode()
					? exceptionStackTraceRows(new Exception(), -1) 
					: null;
			emitTrace(report(action, msg, stc)); 
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