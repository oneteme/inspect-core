package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class FtpRequest2 extends AbstractRequest2 {

	private String protocol; //FTP, FTPS => secure:boolean
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	
	public FtpRequest2(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}
	
	public FtpRequestCallback createCallback() {
		return new FtpRequestCallback(getId());
	}
}
