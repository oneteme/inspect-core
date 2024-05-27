package org.usf.traceapi.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonIgnoreProperties("location")
public class ApiRequest extends RunnableStage {

	private String id; // <= Traceable server
	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domaine
	private int port; // -1 otherwise
	private String path;
	private String query; //text/html, application/json, application/xml,..
	private String contentType; //nullable
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private int status; //0 otherwise
	private long inDataSize; //-1 otherwise
	private long outDataSize; //-1 otherwise
	//v22
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..
	// => in/out Content [type, size, encoding]
}