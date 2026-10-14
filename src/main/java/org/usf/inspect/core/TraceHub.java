package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.usf.inspect.core.ExceptionTrace.fromException;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;

import java.util.List;

import org.slf4j.Logger;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {

	static Logger log = getLogger(TraceHub.class);
	
	default void configure(SchedulingProperties properties, TracePublisher publisher, List<DispatchHook> hooks) { }

	default boolean canCollect() {return false;}

	default void dispatch(InstanceEnvironment instance) throws DispatchException { }

	default void emitTrace(EventTrace trace) { }

	default void emitExceptionTrace(Throwable thrw, TraceUpdate upd, long offset) {
		if(canCollect()) {
			var cfg = getConfiguration().getProperties().getMonitoring().getException();
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