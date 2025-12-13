package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;

public interface Monitor {
	
	default void emit(EventTrace trace) {
		try {
			context().emitTrace(trace);
		}
		catch (Throwable ex) {// do not throw exception
			context().reportError(true, "EventTrace.emit", ex);
		}
	}
	
	default void reportError(boolean stack, String action, Throwable thwr) {
		context().reportError(stack, action, thwr);
	}

	default void reportMessage(boolean stack, String action, String msg) {
		context().reportMessage(stack, action, msg);
	}
	
	default boolean assertStillOpened(Callback callback) {
		if(nonNull(callback)) {
			if(isNull(callback.getEnd())) {
				return true;
			}
			context().reportMessage(true, "assertStillOpened", format("'%s' is already closed", callback.getClass().getSimpleName()));
		}
		context().reportMessage(true, "assertStillOpened", "callback is null");
		return false;
	}

	static boolean assertMonitorNonNull(Object monitor, String action) {
		if (isNull(monitor)) {
			context().reportMessage(false, action, "monitor is null");
			return false;
		}
		return true;
	}
}
