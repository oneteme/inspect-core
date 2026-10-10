package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.ReportEvent.report;
import static org.usf.inspect.core.StackTraceRow.exceptionStackTraceRows;

import java.util.List;

import org.slf4j.Logger;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {

	static Logger log = getLogger(TraceHub.class);
	
	InspectCollectorProperties getConfiguration();
	
	default void configure(InspectCollectorProperties properties, TracePublisher publisher, List<DispatchHook> hooks) { }

	default boolean canCollect() {return false;}

	default void dispatch(InstanceEnvironment instance) throws DispatchException { }

	default void emitTrace(EventTrace trace) { }

	@Deprecated
	default boolean emitTask(DispatchTask task) {return false;}


	default void emitReport(String action, String msg) {
		if(canCollect()) {
			var stc = getConfiguration().isDebugMode()
					? exceptionStackTraceRows(new Exception(), -1) 
					: null;
			emitTrace(report(action, msg, null, stc)); 
		}
	}
	
	default void emitReport(String action, Throwable thrw) {
		emitReport(action, null, thrw);
	}
	
	default void emitReport(String action, String msg, Throwable thrw) {
		if(canCollect()) {
			var exp = nonNull(thrw) ? thrw.getClass().getSimpleName() + ":" + thrw.getMessage() : null;
			StackTraceRow[] stc = null;
			if(getConfiguration().isDebugMode()) {
				stc = exceptionStackTraceRows(nonNull(thrw) ? thrw : new Exception(), -1);
			}
			emitTrace(report(action, msg, exp, stc));
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