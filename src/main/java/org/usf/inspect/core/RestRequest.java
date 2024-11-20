package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.prettyURLFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestRequest extends SessionStage { //APiRequest

	private String id; // <= Traceable server
	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	private String path; //request path
	private String query; //request parameters
	private String contentType; //text/html, application/json, application/xml,..
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private int status; //2xx, 4xx, 5xx, 0 otherwise
	private long inDataSize; //in bytes, -1 unknown
	private long outDataSize; //in bytes, -1 unknown
	private ExceptionInfo exception;
	//v22
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..
	// => in/out Content [type, size, encoding]
	//rest-collector
	
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