package org.usf.traceapi.core;

import static java.util.Objects.nonNull;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RunnableStage extends SessionStage implements MutableStage {

	private String name; //method, title
	private String location; //class, URL
	private ExceptionInfo exception; 
	
	@Override
	String prettyFormat() {
		var s = Objects.isNull(getUser()) ? "" : '<' + getUser() + '>';
		s+= name + "(" + location + ")";
		if(nonNull(exception)) {
			s += exception;
		}
		return s;
	}
}
