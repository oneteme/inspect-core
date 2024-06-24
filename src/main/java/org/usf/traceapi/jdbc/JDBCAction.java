package org.usf.traceapi.jdbc;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, DATABASE, SCHEMA, STATEMENT, METADATA, DISCONNECTION,
	BATCH, EXECUTE, FETCH, //WARN
	SAVEPOINT, COMMIT, ROLLBACK, //TCL
}