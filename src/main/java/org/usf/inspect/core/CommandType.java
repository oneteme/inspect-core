package org.usf.inspect.core;

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
	ACCESS, //cd,
	SETUP, //create, drop, ..
	SCRIPT; //multiple command
	
	public static String merge(String main, CommandType type) {
		if(isNull(type)) {
			return main;
		}
		if(isNull(main)) {
			return type.name();
		}
		return type.name().equals(main) 
				? main 
				: SCRIPT.name();
	}
}