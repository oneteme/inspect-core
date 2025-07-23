package org.usf.inspect.core;

import java.io.File;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void register(InstanceEnvironment instance); //callback ?
    
	void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces);

	void dispatch(File dumpFile); //json
	
	static DispatcherAgent noAgent() {
		
		return new DispatcherAgent() {
			
			@Override
			public void register(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces) throws DispatchException {
				//do nothing
			}
			
			@Override
			public void dispatch(File dumpFile) throws DispatchException {
				//do nothing
			}
		};
		
	}
	
}