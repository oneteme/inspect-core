package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {

	default boolean dispatch(InstanceEnvironment instance) {return false;}

	@Deprecated
	default boolean emitTask(DispatchTask task) {return false;}

	default boolean emitTrace(EventTrace trace) {return false;}

	default boolean emitTraces(List<EventTrace> traces) {return false;}

	default void reportError(String action, Throwable thwr) {}

	default void reportMessage(String action, String msg) {}

	InspectCollectorConfiguration getConfiguration();

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