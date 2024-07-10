package org.usf.inspect.jdbc;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, DISCONNECTION, 
	METADATA, DATABASE, SCHEMA, STATEMENT,
	BATCH, EXECUTE, MORE, FETCH, //+WARN
	SAVEPOINT, COMMIT, ROLLBACK, //TCL
}