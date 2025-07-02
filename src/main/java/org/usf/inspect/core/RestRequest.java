package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.prettyURLFormat;

import java.net.URI;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestRequest extends AbstractRequest<HttpRequestStage> { //APiRequest

	private String id; // <= Traceable server
	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	private String path; //request path
	private String query; //request parameters
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private int status; //2xx, 4xx, 5xx, 0 otherwise
	private long inDataSize; //in bytes, -1 unknown
	private long outDataSize; //in bytes, -1 unknown
	@Deprecated(since = "v1.1", forRemoval = true)
	private ExceptionInfo exception;
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..

	//v1.1.0
	private String bodyContent; //incoming content, //4xx, 5xx only
	// => in/out Content [type, size, encoding]
	//rest-collector
	
	public void setURI(URI uri) {
		setProtocol(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setQuery(uri.getQuery());
	}
	
	@Override
	protected HttpRequestStage createStage() {
		return new HttpRequestStage();
	}
	
	@Override
	public String prettyFormat() {
		var s = '['+method+']'+ prettyURLFormat(getUser(), protocol, host, port, path);
		if(nonNull(query)) {
			s += '?' + query;
		}
		s += " >> " + status;
		if(nonNull(exception)) {
			s += exception;
		}
		return s;
	}
}