package org.usf.traceapi.core.ftp;

public enum FtpAction {
	
	GET, LS, //read
	PUT, MKDIR, RENAME, //write
	CD, CHMOD, CHOWN; //access

}
