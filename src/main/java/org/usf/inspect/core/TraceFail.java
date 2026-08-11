package org.usf.inspect.core;

/**
 * Represents a failed trace state and whether it can be retried.
 * 
 * @param state the failure state description
 * @param retry whether the failed trace can be retried
 * @author u$f
 *
 */
public record TraceFail(String state, boolean retry) { }