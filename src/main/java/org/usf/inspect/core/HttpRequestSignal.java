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
public final class HttpRequestSignal extends AbstractRemoteRequestSignal {

	private String method; //GET, POST, PUT,..
	private String path; //request path
	private String query; //request parameters
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private long dataSize; //in bytes, -1 unknown
	private String contentEncoding; //gzip, compress, identity,..

	public HttpRequestSignal(UUID id, UUID sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withInstant(getStart())
				.withThread(getThreadName())
				.withAction(method)
				.withUrlAsTopic(getProtocol(), getHost(), getPort(), path, query)
				.withUser(getUser())
				.format();
	}
}
