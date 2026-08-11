package org.usf.inspect.core;

/**
 * Represents a trace dispatch task to execute with an exporter.
 */
@FunctionalInterface
public interface DispatchTask { //max retry !
	
	/**
	 * Dispatches traces through the given exporter.
	 *
	 * @param agent the trace exporter to use
	 */
	void dispatch(TraceExporter agent);
}
