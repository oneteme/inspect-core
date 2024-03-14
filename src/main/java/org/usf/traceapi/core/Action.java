package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public enum Action {
	
	CONNECTION, METADATA, STATEMENT,
	EXECUTE, SELECT, RESULTSET, UPDATE, BATCH, //count
	SAVEPOINT, COMMIT, ROLLBACK, FETCH;
}