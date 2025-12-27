package org.usf.inspect.core;

import java.net.URI;
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

	public HttpSessionSignal(String id, Instant start, String threadName) {
		super(id, start, threadName);
	}

	public void setURI(URI uri) {
		setProtocol(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setQuery(uri.getQuery());
	}

	public HttpSessionUpdate createCallback() {
		return new HttpSessionUpdate(getId());
	}
}
