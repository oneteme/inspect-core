package org.usf.inspect.core;

import static org.usf.inspect.core.CommandType.READ;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public enum DirCommand {
	
	LOOKUP(READ), LIST(READ), ATTRIB(READ), SEARCH(READ); //READ
	
	private final CommandType type;
}
