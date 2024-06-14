package org.usf.traceapi.core;

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
public class SessionStage extends Stage implements MutableStage {
	
//	private String name; //method, title
	private String location; //class, URL
	private String user;
	private String threadName;
	
	@Override
	public String toString() {
		var s = getName() + "(" + location + ") {" + duration() + "ms}";
		return nonNull(user) ? '<'+user+'>'+s : s;
	}
}