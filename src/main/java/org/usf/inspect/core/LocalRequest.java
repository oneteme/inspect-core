package org.usf.inspect.core;

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
public class LocalRequest extends AbstractRequest { //TODO extends RequestStage

	private String name; //method, title
	private String type;
	private String location; //class, URL
	private ExceptionInfo exception; 
	
	@JsonCreator public LocalRequest() { }

	LocalRequest(LocalRequest req) {
		super(req);
		this.name = req.name;
		this.type = req.type;
		this.location = req.location;
		this.exception = req.exception;
	}
	
	@Override
	public LocalRequest copy() {
		return new LocalRequest(this);
	}

	@Override
	public String toString() {
		return new EventTraceFormatter()
		.withThread(getThreadName())
		.withCommand(type)
		.withUser(getUser())
		.withLocationAsResource(location, name)
		.withResult(exception)
		.withPeriod(getStart(), getEnd())
		.format();
	}
}