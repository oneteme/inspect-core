package org.usf.inspect.core;

/**
 * Enumerates database lifecycle and execution actions.
 */
public enum DatabaseAction {
	
	CONNECTION, DISCONNECTION, 
	METADATA, //explore
	STATEMENT, BATCH, EXECUTE, FETCH, //+WARN
	SAVEPOINT, COMMIT, ROLLBACK; //TCL
}