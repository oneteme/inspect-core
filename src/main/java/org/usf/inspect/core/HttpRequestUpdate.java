package org.usf.inspect.core;

import java.util.UUID;

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
public final class HttpRequestUpdate extends AbstractRequestUpdate {

	private long dataSize; //in bytes, -1 unknown
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String contentEncoding; //gzip, compress, identity,..
	private String bodyContent; //incoming content, //4xx, 5xx only
	private boolean linked;

	@JsonCreator
	public HttpRequestUpdate(UUID id) {
		super(id);
	}
}
