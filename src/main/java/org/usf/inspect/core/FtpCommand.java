package org.usf.inspect.core;

import static org.usf.inspect.core.CommandType.ACCESS;
import static org.usf.inspect.core.CommandType.EDIT;
import static org.usf.inspect.core.CommandType.EMIT;
import static org.usf.inspect.core.CommandType.READ;
import static org.usf.inspect.core.CommandType.ROLE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public enum FtpCommand {

	CD(ACCESS), //access
	GET(READ), LS(READ), //read
	PUT(EMIT), MKDIR(EDIT), RENAME(EDIT), RM(EDIT), //write
	CHMOD(ROLE), CHOWN(ROLE), CHGRP(ROLE); //role
	
	private final CommandType type;
}
