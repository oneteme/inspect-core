package org.usf.inspect.core;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Metric.prettyDurationFormat;

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
public abstract class AbstractStage implements Metric {

	private String name; //rename to type
	private Instant start;
	private Instant end;
	private ExceptionInfo exception;
	//v1.1
	private String requestId;
//	private String threadName
	
	@Override
	public Metric copy() {
		return this; //!lazy
	}
	
	@Override
	public String toString() {
		var s = prettyFormat();
		if(nonNull(exception)) {
			s += " >> " + exception;
		}
		return s + " " + prettyDurationFormat(this);
	}
	
	abstract String prettyFormat();
}