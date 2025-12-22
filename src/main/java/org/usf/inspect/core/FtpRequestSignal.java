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
public final class FtpRequestSignal extends AbstractRequestSignal {

	private String protocol; //FTP, FTPS => secure:boolean
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	
	public FtpRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}
	
	public FtpRequestUpdate createCallback() {
		return new FtpRequestUpdate(getId());
	}
}
