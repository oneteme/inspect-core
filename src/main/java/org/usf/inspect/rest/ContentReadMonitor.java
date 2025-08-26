package org.usf.inspect.rest;

import java.time.Instant;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface ContentReadMonitor {

	void handle(Instant start, Instant end, long size, byte[] res, Throwable t);
}
