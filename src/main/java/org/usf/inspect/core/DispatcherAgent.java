package org.usf.inspect.core;

import java.io.File;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void dispatch(InstanceEnvironment env);
    
	void dispatch(boolean complete, int attemps, int pending, List<EventTrace> items) throws DispatchException;

	void dispatch(File dumpFile) throws DispatchException; //json
}