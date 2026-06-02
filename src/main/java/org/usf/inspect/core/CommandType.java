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
	SETUP, //create, drop, ..
	ACCESS, //cd,
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