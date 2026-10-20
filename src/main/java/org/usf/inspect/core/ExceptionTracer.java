package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.TraceHub.hub;

import org.usf.inspect.AbstractSessionUpdate;
import org.usf.inspect.ExceptionTrace;
import org.usf.inspect.StackTraceRow;
import org.usf.inspect.TraceUpdate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExceptionTracer {

	public static void emitExceptionTrace(Throwable thrw, TraceUpdate upd, long offset) {
		var cfg = getConfiguration().getProperties().getMonitoring().getException();
		if(cfg.isEnabled() && nonNull(thrw)) {
			var trc = upd instanceof AbstractSessionUpdate 
					? fromException(thrw, cfg.getMaxCauseDepth(), cfg.getMaxStackTraceRows())
					: fromException(thrw, 0, 0);
			if(nonNull(upd)) {
				trc.setTraceId(upd.getId());
				trc.setTraceType(upd.traceType());
			}
			trc.setOffset(offset);
			hub().emitTrace(trc);
		}
	}
	
	static ExceptionTrace fromException(Throwable thrw, int maxCauses, int maxRows) {
		var cause = thrw.getCause();
		return new ExceptionTrace(
				thrw.getClass().getName(), 
				thrw.getMessage(), 
				exceptionStackTraceRows(thrw, maxRows),
				maxCauses != 0 && nonNull(cause) && thrw != cause ? fromException(cause, --maxCauses, maxRows) : null);
	}


	public static StackTraceRow[] exceptionStackTraceRows(Throwable thrw, int maxRows) {
		return nonNull(thrw) ? exceptionStackTraceRows(thrw.getStackTrace(), maxRows) : null;
	}
	
	public static StackTraceRow[] exceptionStackTraceRows(StackTraceElement[] stack, int maxRows) {
		if(isNull(stack) || maxRows == 0) {
			return null;
		}
		var rows = new StackTraceRow[maxRows < 0 ? stack.length : min(maxRows, stack.length)];
		for(var i=0; i<rows.length; i++) {
			rows[i] = new StackTraceRow(
					stack[i].getClassName(),
					stack[i].getMethodName(), 
					stack[i].getLineNumber());
		}
		return rows;
	}
}
