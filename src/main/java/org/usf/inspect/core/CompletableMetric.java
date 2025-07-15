package org.usf.inspect.core;

import static java.util.Objects.nonNull;

/**
 * 
 * @author u$f
 *
 */
public interface CompletableMetric extends Metric {
	
	String getId();

	CompletableMetric copy();

	default boolean wasCompleted(){
		return nonNull(getEnd());
	}
	
	default void runSynchronized(Runnable r) {
		synchronized (this) {
			r.run();
		}
	}
	
	default void runSynchronizedIfNotComplete(Runnable r) {
		synchronized (this) {
			if(!wasCompleted()) {
				r.run();
			}
		}
	}
}
