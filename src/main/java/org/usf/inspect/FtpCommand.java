package org.usf.inspect;

import static org.usf.inspect.CommandType.CONTEXT;
import static org.usf.inspect.CommandType.EDIT;
import static org.usf.inspect.CommandType.EMIT;
import static org.usf.inspect.CommandType.READ;
import static org.usf.inspect.CommandType.ROLE;

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

	CD(CONTEXT), 
	GET(READ), LS(READ), //read
	PUT(EMIT), MKDIR(EDIT), RENAME(EDIT), RM(EDIT), //write
	CHMOD(ROLE), CHOWN(ROLE), CHGRP(ROLE); //role
	
	private final CommandType type;
}
