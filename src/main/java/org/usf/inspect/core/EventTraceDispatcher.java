package org.usf.inspect.core;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface EventTraceDispatcher<T> {

	boolean dispatch(boolean complete, int attemps, int pending, List<T> items) throws Exception;
}