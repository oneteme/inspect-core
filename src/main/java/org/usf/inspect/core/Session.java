package org.usf.inspect.core;

import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends CompletableMetric {
	
	void lock(); //must be called before session end
	
	void unlock();
	
	void updateMask(RequestMask mask);
	
	default void updateContext() {
		setCurrentSession(this);
	}
	
	default void releaseContext() {
		releaseSession(this);
	}
}
