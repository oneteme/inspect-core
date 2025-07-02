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
public class LocalRequest extends AbstractRequest<AbstractStage> { //TODO extends RequestStage

	private String name; //method, title
	private String location; //class, URL
	private String type;
	private ExceptionInfo exception; 
	
	@Override
	protected AbstractStage createStage() {
		throw new UnsupportedOperationException("LocalRequest does not support append stage");
	}
	
	@Override
	String prettyFormat() {
		var s = isNull(type) ? "" : '['+type+']';
		if(nonNull(getUser())) {
			s+= '<' + getUser() + '>';
		}
		s+= name + "(" + location + ")";
		if(nonNull(exception)) {
			s += " >> " + exception;
		}
		return s;
	}
}