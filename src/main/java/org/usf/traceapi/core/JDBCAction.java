package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, STATEMENT, RESULTSET, METADATA,
	BATCH, EXECUTE, FETCH,
	SAVEPOINT, COMMIT, ROLLBACK; //TCL
}