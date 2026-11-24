package org.usf.inspect;

/**
 * 
 * @author u$f
 *
 */
public enum DatabaseAction {
	
	CONNECTION, DISCONNECTION, ABORT, 
	METADATA, //DESCRIBE
	STATEMENT, BATCH, EXECUTE, FETCH, //+WARN
	
}