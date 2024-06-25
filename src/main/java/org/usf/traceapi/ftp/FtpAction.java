package org.usf.traceapi.ftp;

/**
 * 
 * @author u$f
 *
 */
public enum FtpAction {
	
	CONNECTION, DISCONNECTION, 
	CD, //access
	GET, LS, //read
	PUT, MKDIR, RENAME, RM, //write
	CHMOD, CHOWN, CHGRP; //role
}