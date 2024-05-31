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
	@Deprecated(forRemoval = true, since = "17") RESULTSET,
	@Deprecated(forRemoval = true, since = "17") SELECT,
	@Deprecated(forRemoval = true, since = "17") UPDATE,
	@Deprecated(forRemoval = true, since = "17") SQL;
}