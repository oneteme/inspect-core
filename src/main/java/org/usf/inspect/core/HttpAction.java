package org.usf.inspect.core;

/**
 * Enumerates HTTP request processing actions.
 */
public enum HttpAction {

	PRE_PROCESS, DEFERRED, PROCESS, POST_PROCESS, 
	ASSEMBLY, EXCHANGE, STREAM;
}
