package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorReporter.reporter;

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
	
	default void runSynchronizedIfNotComplete(Runnable r) {
		synchronized (this) {
			if(!wasCompleted()) {
				r.run();
			}
		}
	}

	default void assertWasNotCompleted(){
		if(nonNull(getEnd())) {
			reporter().action("assertWasNotCompleted").trace(this).emit();
		}
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
