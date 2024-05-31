package org.usf.traceapi.core;

import static java.lang.String.join;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class FtpRequestStage extends Stage {

	private String[] args;
	
	@Override
	public String toString() {
		return super.toString() + " | " + join(",", args);
	}
}