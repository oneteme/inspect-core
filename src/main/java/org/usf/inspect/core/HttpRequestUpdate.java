package org.usf.inspect.core;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable update data for an HTTP request.
 */
@Getter
@Setter
public final class HttpRequestUpdate extends AbstractRequestUpdate {

	private int status; //2xx, 4xx, 5xx, 0 otherwise 
	private long dataSize; //in bytes, -1 unknown
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String contentEncoding; //gzip, compress, identity,..
	private String bodyContent; //incoming content, //4xx, 5xx only
	private boolean linked;

	/**
	 * Creates an HTTP request update.
	 *
	 * @param id the request identifier
	 */
	@JsonCreator
	public HttpRequestUpdate(String id) {
		super(id);
	}

	/**
	 * Creates an HTTP request stage.
	 *
	 * @param type the HTTP action
	 * @param start the stage start time
	 * @param end the stage end time
	 * @param t the failure cause, if any
	 * @return the created stage
	 */
	public HttpRequestStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return createStage(type, start, end, null, t, HttpRequestStage::new);
	}
}
