package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
public record TraceFail(boolean retry, String cause) { }