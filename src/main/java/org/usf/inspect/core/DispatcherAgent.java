package org.usf.inspect.core;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.List;

/**
 * 
 * @author u$f
 *
 */
public interface DispatcherAgent {
	
	void dispatch(InstanceEnvironment instance); //callback ?
    
	List<EventTrace> dispatch(boolean complete, List<EventTrace> traces);

	void dispatch(File dumpFile);
	
	static DispatcherAgent noAgent() {
		
		return new DispatcherAgent() {
			
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