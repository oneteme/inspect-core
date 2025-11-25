package org.usf.inspect.core;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void dispatch(InstanceEnvironment instance); //callback ?
    
	List<EventTrace> dispatch(boolean complete, int attempts, int pending, List<EventTrace> traces);

	void dispatch(int attempts, File dumpFile);
	
	static DispatcherAgent noAgent() {
		
		return new DispatcherAgent() {
			
			@Override
			public void dispatch(InstanceEnvironment env) {
				//do nothing
			}
			
			@Override
			public List<EventTrace> dispatch(boolean complete, int attempts, int pending, List<EventTrace> traces) {
				return emptyList();
			}
			
			@Override
			public void dispatch(int attempts, File dumpFile) {
				//do nothing
			}
		};
	}
}