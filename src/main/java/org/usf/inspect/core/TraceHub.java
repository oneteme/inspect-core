package org.usf.inspect.core;

import org.usf.inspect.EventTrace;
import org.usf.inspect.InstanceEnvironment;

/**
 * 
 * @author u$f
 *
 */
public interface TraceHub {

	default void dispatch(InstanceEnvironment instance) throws DispatchException { }

	default void emitTrace(EventTrace trace) { }

	static TraceHub hub() {
		return Holder.INSTANCE;
	}

	final class Holder {
		static TraceHub INSTANCE = new TraceDispatcherHub();
		private Holder() {}
	}
}