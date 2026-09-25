package org.usf.inspect.core;

import java.io.File;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface TraceExporter {
	
	void dispatch(InstanceEnvironment instance); //callback ?
    
	List<EventTrace> dispatch(boolean complete, List<EventTrace> traces);

	@Deprecated(forRemoval = true, since = "v1.2")
	default void dispatch(File dumpFile) {}

}