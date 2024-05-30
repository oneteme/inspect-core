package org.usf.traceapi.core;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class FtpAction implements Metric {
	
	private String name;
	private Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	private String[] args;
	
	@Override
	public String toString() {
		return name + " {" + duration() + "ms}";
	}
}
