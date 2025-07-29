package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Objects;

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
	
	default boolean runSynchronizedIfNotComplete(Runnable r) {
		synchronized (this) {
			if(!wasCompleted()) {
				r.run();
				return true;
			}
		}
		return false;
	}

	static boolean areEquals(CompletableMetric o1, Object o2) {
		if(o1 == o2) {
			return true;
		}
		if(isNull(o1) || isNull(o2)) {
			return false;
		}
		return o1.getClass() == o2.getClass()
				&& Objects.equals(o1.getId(), ((CompletableMetric)o2).getId()); // class are Equals
	}
	
	static int hashCodeOf(CompletableMetric o) {
		return isNull(o) || isNull(o.getId()) ? 0 : o.getId().hashCode();
	}
}
