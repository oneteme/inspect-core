package org.usf.traceapi.core;

/**
 * 
 * @author u$f
 *
 */
public enum JDBCAction {
	
	CONNECTION, STATEMENT, METADATA,
	BATCH, EXECUTE, FETCH,
	SAVEPOINT, COMMIT, ROLLBACK, //TCL
	@Deprecated(forRemoval = true) RESULTSET,
	@Deprecated(forRemoval = true) SELECT,
	@Deprecated(forRemoval = true) UPDATE,
	@Deprecated(forRemoval = true) SQL;
}