package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public enum SqlAction {
	
	CONNECTION, STATEMENT, RESULTSET, METADATA,
	BATCH, EXECUTE, FETCH,
	SAVEPOINT, COMMIT, ROLLBACK; //TCL
}