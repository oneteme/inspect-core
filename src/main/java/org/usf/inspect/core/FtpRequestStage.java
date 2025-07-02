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
public final class FtpRequestStage extends AbstractStage {

	private String[] args;
	
	@Override
	public String prettyFormat() {
		var s = getName();
		if(nonNull(args)) {
			s += '(' + join(",", args) + ')';
		}
		return s;
	}
}