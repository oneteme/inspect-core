package org.usf.inspect.core;

import java.net.URI;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Signal that describes the start of an HTTP request.
 */
@Getter
@Setter
public final class HttpRequestSignal extends AbstractRequestSignal {

	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	private String path; //request path
	private String query; //request parameters
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private long dataSize; //in bytes, -1 unknown
	private String contentEncoding; //gzip, compress, identity,..

	/**
	 * Creates an HTTP request signal.
	 *
	 * @param id the request identifier
	 * @param sessionId the owning session identifier
	 * @param start the request start time
	 * @param threadName the originating thread name
	 */
	public HttpRequestSignal(String id, String sessionId, Instant start, String threadName) {
		super(id, sessionId, start, threadName);
	}

	/**
	 * Copies URI parts into this request signal.
	 *
	 * @param uri the request URI
	 */
	public void setURI(URI uri) {
		setProtocol(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setQuery(uri.getQuery());
	}

	/**
	 * Creates the mutable update associated with this request.
	 *
	 * @return the HTTP request update
	 */
	public HttpRequestUpdate createCallback() {
		return new HttpRequestUpdate(getId());
	}
}
