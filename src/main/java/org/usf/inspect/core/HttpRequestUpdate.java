package org.usf.inspect.core;

import java.time.Instant;
import java.util.function.ToIntFunction;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;


import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ErrorCode.UNKNOWN_ERROR;
import static org.usf.inspect.core.ProtocolErrorHandler.mainCauseException;

/**
 * 
 * @author u$f
 *
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
	HttpErrorHandler httpErrorHandler= new HttpErrorHandler();
	private int failureCode;

	@JsonCreator
	public HttpRequestUpdate(String id) {
		super(id);
	}

	public HttpRequestStage createStage(HttpAction type, Instant start, Instant end, Throwable t, ToIntFunction<Throwable> fn) {
		Throwable ex = null;
		if(nonNull(t)) {
			ex = ExceptionInfo.rootCauseException(t);
			try{
			failureCode = fn.applyAsInt(ex);
			} catch (Exception e) {
			failureCode = UNKNOWN_ERROR.getCode();
		    }
	    }
		return createStage(type, start, end, null, t, HttpRequestStage::new);
	}
}
