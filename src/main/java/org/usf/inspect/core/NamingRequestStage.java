package org.usf.inspect.core;

import static java.lang.String.join;
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
public class NamingRequestStage extends RequestStage {
	
	private String[] args;
	//int count !?
	
	@Override
	protected String prettyFormat() {
		var s = getName();
		if(nonNull(args)) {
			s += '(' + join(",", args) + ')';
		}
		return s;
	}
}
