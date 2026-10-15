package org.usf.inspect;

/**
 * 
 * @author u$f
 *
 */
public enum DatabaseAction {
	
	CONNECTION, DISCONNECTION, 
	METADATA, //explore
	STATEMENT, BATCH, EXECUTE, FETCH, //+WARN
	SAVEPOINT, COMMIT, ROLLBACK; //TCL
}