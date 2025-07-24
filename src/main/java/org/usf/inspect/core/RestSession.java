package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestSession extends AbstractSession {

	@Delegate
	@JsonIgnore
	private final RestRequest rest = new RestRequest();
	private String name; //api name
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache
	//v1.1
	private ExceptionInfo exception;

	@Override
	public RestSession copy() {
		var ses = new RestSession();
		rest.copyIn(ses.rest);
		ses.setName(name);
		ses.setUserAgent(userAgent);
		ses.setCacheControl(cacheControl);
		ses.setException(exception);
		return ses;
	}
	
	public HttpSessionStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return rest.createStage(type, start, end, t, HttpSessionStage::new);
	}
	
	@Override
	public String toString() {
		var s = rest.toString();
		if(isNull(getBodyContent()) && nonNull(exception)) {
			s += " >> " + exception;
		}
		return s;
	}
}