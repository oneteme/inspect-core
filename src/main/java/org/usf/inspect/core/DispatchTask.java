package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface DispatchTask { //max retry !
	
	void dispatch(TraceExporter agent);
}
