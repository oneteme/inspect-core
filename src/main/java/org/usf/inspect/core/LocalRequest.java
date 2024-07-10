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
public class LocalRequest extends SessionStage implements MutableStage {

	private String name; //method, title
	private String location; //class, URL
	private ExceptionInfo exception; 
	
	@Override
	String prettyFormat() {
		var s = isNull(getUser()) ? "" : '<' + getUser() + '>';
		s+= name + "(" + location + ")";
		if(nonNull(exception)) {
			s += " >> " + exception;
		}
		return s;
	}
}