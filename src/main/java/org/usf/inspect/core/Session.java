package org.usf.inspect.core;

import static java.util.UUID.randomUUID;

/**
 * 
 * @author u$f
 *
 */
public interface Session extends LazyMetric {
	
	String getId();

	void lock(); //must be called before session end
	
	void unlock();
	
	static String nextId() {
		return randomUUID().toString();
	}
}
