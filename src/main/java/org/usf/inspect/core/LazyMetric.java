package org.usf.inspect.core;

import static java.util.Objects.nonNull;

/**
 * 
 * @author u$f
 *
 */
public interface LazyMetric extends Metric {
	
	String getId();

	LazyMetric copy();

	default boolean wasCompleted(){
		return nonNull(getEnd());
	}
	
	default void lazy(Runnable r) {
		synchronized (this) {
			r.run();
		}
	}
}
