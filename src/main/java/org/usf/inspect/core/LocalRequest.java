package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
	
	@Override
	public LocalRequest copy() {
		var req = new LocalRequest();
		copyIn(req);
		return req;
	}
	
	void copyIn(LocalRequest req) {
		req.setId(getId());
		req.setStart(getStart());
		req.setEnd(getEnd());
		req.setUser(getUser());
		req.setThreadName(getThreadName());
		req.setSessionId(getSessionId());
		req.setName(name);
		req.setType(type);
		req.setLocation(location);
		req.setException(exception);
	}
	
	@Override
	String prettyFormat() {
		var s = isNull(type) ? "" : '['+type+']';
		if(nonNull(getUser())) {
			s+= '<' + getUser() + '>';
		}
		s+= " " + location + "." + name;
		if(nonNull(exception)) {
			s += " >> " + exception;
		}
		return s;
	}
}