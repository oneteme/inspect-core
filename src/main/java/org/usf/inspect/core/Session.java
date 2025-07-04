package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends LazyMetric {
	
	void lock(); //must be called before session end
	
	void unlock();
}
