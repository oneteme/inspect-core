package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.Metric.prettyDurationFormat;

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
public abstract class RequestStage implements Metric {

	private String name;
	private Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	
	@Override
	public String toString() {
		var s = prettyFormat();
		if(nonNull(exception)) {
			s += exception;
		}
		return s + " " + prettyDurationFormat(this);
	}
	
	abstract String prettyFormat();
}