package org.usf.inspect.core;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents update data collected while processing an HTTP session.
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class HttpSessionUpdate extends AbstractSessionUpdate implements HasStage {
	
	@JsonIgnore 
	private final AtomicInteger stageCounter = new AtomicInteger();

	private int status; //2xx, 4xx, 5xx, 0 otherwise 
	private long dataSize; //in bytes, -1 unknown
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String contentEncoding; //gzip, compress, identity,..
	private String cacheControl; //max-age, no-cache
	private String bodyContent; //incoming content, //4xx, 5xx only
	
	/**
	 * Creates a new HTTP session update for the given session identifier.
	 * 
	 * @param id the unique session identifier
	 */
	@JsonCreator
	public HttpSessionUpdate(String id) {
		super(id);
	}

	/**
	 * Creates a new stage entry for the current HTTP session update.
	 * 
	 * @param type the action type represented by the stage
	 * @param start the stage start instant
	 * @param end the stage end instant
	 * @param t the error captured for the stage, or {@code null} when none occurred
	 * @return a new HTTP session stage initialized with the supplied values
	 */
	public HttpSessionStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return createStage(type, start, end, null, t, HttpSessionStage::new);
	}
}
