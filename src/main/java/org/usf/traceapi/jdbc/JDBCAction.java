package org.usf.traceapi.jdbc;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, STATEMENT, METADATA, DISCONNECTION,
	BATCH, EXECUTE, FETCH,
	SAVEPOINT, COMMIT, ROLLBACK, //TCL
}