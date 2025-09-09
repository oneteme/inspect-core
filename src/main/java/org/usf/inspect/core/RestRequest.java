package org.usf.inspect.core;

import java.net.URI;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestRequest extends AbstractRequest { //APiRequest

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
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..

	//v1.1
	private String bodyContent; //incoming content, //4xx, 5xx only
	private boolean linked;
	// => in/out Content [type, size, encoding]
	//rest-collector
	
	@JsonCreator public RestRequest() { }

	public RestRequest(RestRequest req) {
		super(req);
		this.protocol = req.protocol;
		this.host = req.host;
		this.port = req.port;
		this.method = req.method;
		this.path = req.path;
		this.query = req.query;
		this.contentType = req.contentType;
		this.authScheme = req.authScheme;
		this.status = req.status;
		this.inDataSize = req.inDataSize;
		this.outDataSize = req.outDataSize;
		this.inContentEncoding = req.inContentEncoding;
		this.outContentEncoding = req.outContentEncoding;
		this.bodyContent = req.bodyContent;
	}
	
	public HttpRequestStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return createStage(type, start, end, null, t, HttpRequestStage::new);
	}
	
	public void setURI(URI uri) {
		setProtocol(uri.getScheme());
		setHost(uri.getHost());
		setPort(uri.getPort());
		setPath(uri.getPath());
		setQuery(uri.getQuery());
	}

	@Override
	public RestRequest copy() {
		return new RestRequest(this);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withAction(method)
		.withUser(getUser())
		.withUrlAsTopic(protocol, host, port, path, query)
		.withStatus(status+"")
		.withResult(bodyContent)
		.withPeriod(getStart(), getEnd())
		.format();
	}
	
	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}