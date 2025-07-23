package org.usf.inspect.core;

import java.io.File;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void dispatch(InstanceEnvironment instance); //callback ?
    
	void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces);

	void dispatch(int attemps, File dumpFile); //json
	
	static DispatcherAgent noAgent() {
		
		return new DispatcherAgent() {
			
			@Override
			public void dispatch(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces) throws DispatchException {
				//do nothing
			}
			
			@Override
			public void dispatch(int attemps, File dumpFile) throws DispatchException {
				//do nothing
			}
		};
	}
}