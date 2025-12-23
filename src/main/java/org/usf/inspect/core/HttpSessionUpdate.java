package org.usf.inspect.core;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
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
	private String bodyContent; //incoming content, //4xx, 5xx only
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache
	
	@JsonCreator
	public HttpSessionUpdate(String id) {
		super(id);
	}

	public HttpSessionStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return createStage(type, start, end, null, t, HttpSessionStage::new);
	}
}
