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
public class HttpSession2 extends AbstractSession2 {
	
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
	private String cacheControl; //max-age, no-cache
	private ExceptionInfo exception; //must replace failed

	public HttpSession2(String id, Instant start, String threadName) {
		super(id, start, threadName);
	}

	public HttpSessionCallback createCallback() {
		return new HttpSessionCallback(getId());
	}
}
