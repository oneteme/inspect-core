package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Signal that describes the start of an FTP request.
 */
@Getter
@Setter
public final class FtpRequestSignal extends AbstractRequestSignal {

	private String protocol; //FTP, FTPS => secure:boolean
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	
	/**
	 * Creates an FTP request signal.
	 *
	 * @param id the request identifier
	 * @param sessionId the owning session identifier
	 * @param start the request start time
	 * @param threadName the originating thread name
	 */
	public FtpRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}
	
	/**
	 * Creates the mutable update associated with this request.
	 *
	 * @return the FTP request update
	 */
	public FtpRequestUpdate createCallback() {
		return new FtpRequestUpdate(getId());
	}
}
