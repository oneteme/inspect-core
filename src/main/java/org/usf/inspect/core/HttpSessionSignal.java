package org.usf.inspect.core;

import java.net.URI;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the initial signal captured for an incoming HTTP session.
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class HttpSessionSignal extends AbstractSessionSignal {
	
	//HttpRequest
	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	private String path; //request path
	private String query; //request parameters
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private long dataSize; //in bytes, -1 unknown
	private String contentEncoding; //gzip, compress, identity,..
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private boolean linked;

	/**
	 * Creates a new HTTP session signal for the supplied session metadata.
	 * 
	 * @param id the unique session identifier
	 * @param start the session start instant
	 * @param threadName the name of the thread that started the session
	 */
	public HttpSessionSignal(String id, Instant start, String threadName) {
		super(id, start, threadName);
	}

	/**
	 * Copies the components of the given URI into this HTTP session signal.
	 * 
	 * @param uri the URI to extract HTTP request information from
	 */
	public void setURI(URI uri) {
		setProtocol(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setQuery(uri.getQuery());
	}

	/**
	 * Creates a callback update object for the current HTTP session.
	 * 
	 * @return a new update bound to this session identifier
	 */
	public HttpSessionUpdate createCallback() {
		return new HttpSessionUpdate(getId());
	}
}
