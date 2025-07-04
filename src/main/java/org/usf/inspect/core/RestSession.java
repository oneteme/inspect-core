package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

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
@JsonTypeName("rest")
public class RestSession extends AbstractSession {

	@Delegate
	@JsonIgnore
	private final RestRequest rest = new RestRequest();
	private String name; //api name
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache

	private ExceptionInfo exception;

	@Override
	public RestSession copy() {
		var ses = new RestSession();
		rest.copyIn(ses.rest);
		return ses;
	}
	
	@Override
	public String toString() {
		var s = rest.toString();
		if(nonNull(exception)) {
			s += " >> " + exception;
		}
		return s;
	}
}