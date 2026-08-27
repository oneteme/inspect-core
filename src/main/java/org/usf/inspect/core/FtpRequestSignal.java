package org.usf.inspect.core;

import java.time.Instant;
import java.util.UUID;

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
	
	public FtpRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}
	
	public FtpRequestUpdate createCallback() {
		return new FtpRequestUpdate(getId());
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withUrlAsTopic(protocol, host, port, null, null)
				.withUser(getUser())
				.format();
	}
}
