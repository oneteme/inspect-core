package org.usf.inspect.core;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.List;

/**
 * Exports collected traces and related instance data to an external destination.
 * 
 * @author u$f
 *
 */
public interface TraceExporter {
	
	/**
	 * Dispatches instance metadata to the exporter.
	 * 
	 * @param instance the instance environment to export
	 */
	void dispatch(InstanceEnvironment instance); //callback ?
    
	/**
	 * Dispatches trace entries to the exporter.
	 * 
	 * @param complete whether the trace batch is complete
	 * @param traces the trace entries to export
	 * @return the traces that remain after the dispatch operation
	 */
	List<EventTrace> dispatch(boolean complete, List<EventTrace> traces);

	/**
	 * Dispatches a dumped trace file to the exporter.
	 * 
	 * @param dumpFile the dump file to export
	 */
	void dispatch(File dumpFile);
	
	/**
	 * Creates an exporter implementation that ignores all dispatched content.
	 * 
	 * @return a no-op trace exporter
	 */
	static TraceExporter noExporter() {
		
		return new TraceExporter() {
			
			@Override
			public void dispatch(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public List<EventTrace> dispatch(boolean complete, List<EventTrace> traces) {
				return emptyList();
			}
			
			@Override
			public void dispatch(File dumpFile) {
				//do nothing
			}
		};
	}
}