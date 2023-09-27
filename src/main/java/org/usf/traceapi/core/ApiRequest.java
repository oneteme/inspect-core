package org.usf.traceapi.core;

import com.fasterxml.jackson.annotation.JsonCreator;
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

	private final String id;
	private String method;
	private String protocol;
	private String host;
	private int port;
	private String path;
	private String query; //nullable
	private String contentType; //nullable
	private String authScheme; //nullable   Basic, Bearer, Digest, OAuth, ..
	private int status; // 0 else
	private long inDataSize;
	private long outDataSize;
	
	@JsonCreator
	public ApiRequest(String id) {
		this.id = id;
	}

	@Override
	public String getLocation() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setLocation(String location) {
		throw new UnsupportedOperationException();
	}
}