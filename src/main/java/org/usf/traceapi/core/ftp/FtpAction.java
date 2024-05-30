package org.usf.traceapi.core.ftp;

/**
 * 
 * @author u$f
 *
 */
public enum FtpAction {
	
	CONNECTION, DISCONNECTION,
	GET, LS, //read
	PUT, MKDIR, RENAME, RM, //write
	CD, CHMOD, CHOWN, CHGRP; //access

}