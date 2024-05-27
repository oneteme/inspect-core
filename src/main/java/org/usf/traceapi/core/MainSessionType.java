package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public enum MainSessionType {

	@Deprecated(forRemoval = true, since = "v22")
	WEBAPP,
	VIEW, //replace webapp
	BATCH,
	//v22
	STARTUP;
}