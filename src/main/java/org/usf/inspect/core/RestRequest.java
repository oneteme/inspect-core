package org.usf.inspect.core;

import static java.lang.String.format;
import static org.usf.inspect.core.InspectContext.context;

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
	// => in/out Content [type, size, encoding]
	//rest-collector
	
	public HttpRequestStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return createStage(type, start, end, t, HttpRequestStage::new);
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
		var req = new RestRequest();
		copyIn(req);
		return req;
	}
	
	void copyIn(RestRequest req) {
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setProtocol(protocol);
		req.setHost(host);
		req.setPort(port);
		req.setMethod(method);
		req.setPath(path);
		req.setQuery(query);
		req.setContentType(contentType);
		req.setAuthScheme(authScheme);
		req.setStatus(status);
		req.setInDataSize(inDataSize);
		req.setOutDataSize(outDataSize);
		req.setInContentEncoding(inContentEncoding);
		req.setOutContentEncoding(outContentEncoding);
		req.setBodyContent(bodyContent);
	}
	
	public void assertRemoteID(String id) {
		if(!getId().equals(id)) {
			context().reportError(format("req.id='%s' <> ses.id='%s'", getId(), id));
		}
	}

	@Override
	public String toString() {
		return new TraceFormatter()
		.withThread(getThreadName())
		.withCommand(method)
		.withUser(getUser())
		.withUrlAsResource(protocol, host, port, path, query)
		.withStatus(status+"")
		.withResult(bodyContent)
		.withPeriod(getStart(), getEnd())
		.format();
	}
}