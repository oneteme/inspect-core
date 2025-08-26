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
	
	void updateRequestsMask(RequestMask mask);
	
	default Session updateContext() {
		setCurrentSession(this);
		return this;
	}
	
	default Session releaseContext() {
		releaseSession(this);
		return this;
	}
}
