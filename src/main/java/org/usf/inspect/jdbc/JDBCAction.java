package org.usf.inspect.jdbc;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, DISCONNECTION,
	METADATA, PRODUCT, DRIVER, CATALOG, SCHEMA, TABLE, COLUMN, KEYS, //reflection
	STATEMENT,
	BATCH, EXECUTE, FETCH, //+WARN
	SAVEPOINT, COMMIT, ROLLBACK, //TCL
}