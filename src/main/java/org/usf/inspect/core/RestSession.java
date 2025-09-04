package org.usf.inspect.core;

import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
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

	@JsonIgnore
	@Delegate(excludes = EventTrace.class) //emit(this)
	private final RestRequest rest;
	private String name; //api name
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache
	//v1.1
	private ExceptionInfo exception;
	//location: controllerClass.method

	@JsonCreator 
	public RestSession() {
		this.rest = new RestRequest();
	}

	RestSession(RestSession ses) {
		super(ses);
		this.rest = new RestRequest(ses.rest);
		this.name = ses.name;
		this.userAgent = ses.userAgent;
		this.cacheControl = ses.cacheControl;
		this.exception = ses.exception;
	}

	public HttpSessionStage createStage(HttpAction type, Instant start, Instant end, Throwable t) {
		return rest.createStage(type, start, end, null, t, HttpSessionStage::new);
	}

	@Override
	public RestSession copy() {
		return new RestSession(this);
	}

	@Override
	public RestSession updateContext() {
		setCurrentSession(this);
		return this;
	}

	@Override
	public RestSession releaseContext() {
		releaseSession(this);
		return this;
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
				.withThread(getThreadName())
				.withAction(getMethod())
				.withUser(getUser())
				.withUrlAsTopic(getProtocol(), getHost(), getPort(), getPath(), getQuery())
				.withStatus(getStatus()+"")
				.withResult(exception)
				.withPeriod(getStart(), getEnd())
				.format();
	}

	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}

	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}