package org.usf.inspect;

import static java.util.Objects.isNull;

/**
 * 
 * @author u$f
 *
 */
public enum CommandType {

	READ, //read, get, select, search
	EDIT, //rename, delete, update, ..
	EMIT, //insert, send, push, publish, ..
	ROLE, // grant, chmod, ..
	SETUP, //create, drop, ..
	SCRIPT, //multiple command
	CONTEXT, //cd, set, get
	TRANSACTION; //begin, commit, rollback, savepoint, release
	
	public static String merge(String main, CommandType type) {
		if(isNull(type)) {
			return main;
		}
		if(isNull(main) || isNeutral(main)) { //override neutral type
			return type.name();
		}
		return type.name().equals(main) || isNeutral(type)
				? main 
				: SCRIPT.name();
	}
	
	public static boolean isNeutral(CommandType type) {
		return type == CONTEXT || type == TRANSACTION;
	}
	
	static boolean isNeutral(String type) {
		return CONTEXT.name().equals(type) || TRANSACTION.name().equals(type);
	}
}