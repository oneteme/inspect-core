package org.usf.inspect.core;

import java.io.File;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void dispatch(InstanceEnvironment instance); //callback ?
    
	void dispatch(boolean complete, int attempts, int pending, EventTrace[] traces);

	void dispatch(int attempts, File dumpFile);
	
	static DispatcherAgent noAgent() {
		
		return new DispatcherAgent() {
			
			@Override
			public void dispatch(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public void dispatch(boolean complete, int attempts, int pending, EventTrace[] traces) throws DispatchException {
				//do nothing
			}
			
			@Override
			public void dispatch(int attempts, File dumpFile) throws DispatchException {
				//do nothing
			}
		};
	}
}