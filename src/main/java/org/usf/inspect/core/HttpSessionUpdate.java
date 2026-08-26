package org.usf.inspect.core;

import java.util.UUID;
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

	private long dataSize; //in bytes, -1 unknown
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String contentEncoding; //gzip, compress, identity,..
	private String cacheControl; //max-age, no-cache
	private String bodyContent; //incoming content, //4xx, 5xx only
	
	@JsonCreator
	public HttpSessionUpdate(UUID id) {
		super(id);
	}
	
	public HttpSessionStage createStage() {
		return new HttpSessionStage(getId(), getStageCounter().incrementAndGet());
	}
}
