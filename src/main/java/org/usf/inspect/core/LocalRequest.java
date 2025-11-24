package org.usf.inspect.core;

import static org.usf.inspect.core.Helper.formatLocation;

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
public class LocalRequest extends AbstractRequest {

	private String name; //title, topic
	private String type;
	private String location; //class.method, URL
	private ExceptionInfo exception; 
	
	@JsonCreator public LocalRequest() { }

	LocalRequest(LocalRequest req) {
		super(req);
		this.name = req.name;
		this.type = req.type;
		this.location = req.location;
		this.exception = req.exception;
	}
	
	public void setLocation(String className, String methodName) {
		this.location = formatLocation(className, methodName);
	}
	
	@Override
	public LocalRequest copy() {
		return new LocalRequest(this);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withAction(type)
		.withUser(getUser())
		.withLocationAsTopic(location, name)
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