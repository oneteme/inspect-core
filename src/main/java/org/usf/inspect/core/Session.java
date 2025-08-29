package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends CompletableMetric {
	
	void lock(); //must be called before session end
	
	void unlock();
	
	void updateRequestsMask(RequestMask mask);
	
	Session updateContext();
	
	Session releaseContext();
}
