package org.usf.inspect.core;

import static java.util.Objects.isNull;

/**
 * Defines high-level categories for traced commands.
 */
public enum CommandType {

	READ, //read, get, select, search
	EDIT, //rename, delete, update, ..
	EMIT, //insert, send, push, publish, ..
	ROLE, // grant, chmod, ..
	SETUP, //create, drop, ..
	SCRIPT, //multiple command
	CONTEXT, //cd, set, get
	@Deprecated
	ACCESS; 
	
	/**
	 * Merges a primary command name with a derived command type.
	 *
	 * @param main the existing command name
	 * @param type the command type to merge
	 * @return the merged command name
	 */
	public static String merge(String main, CommandType type) {
		if(isNull(type)) {
			return main;
		}
		if(isNull(main) || main.equals(CONTEXT.name())) {
			return type.name();
		}
		return type.name().equals(main) || type == CONTEXT
				? main 
				: SCRIPT.name();
	}
}