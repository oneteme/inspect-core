package org.usf.inspect.core;

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
public abstract class SessionStage<T> implements Metric {
	
	private String user;
	private Instant start;
	private Instant end;
	private String threadName;
	
	@Override
	public String toString() {
		return prettyFormat() + " " + prettyDurationFormat(this);
	}
	
	abstract boolean append(T stage);
	
	abstract String prettyFormat();
}