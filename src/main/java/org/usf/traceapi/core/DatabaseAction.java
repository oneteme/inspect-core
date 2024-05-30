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
public final class DatabaseAction implements Metric {

	private String name; //rename to name
	private Instant start;
	private Instant end;
	private ExceptionInfo exception; 
	private long[] count; // only for BATCH|UPDATE|FETCH

	@Deprecated(forRemoval = true, since = "v22")
	public void setType(String type) {
		this.name = type;
	}

	@Deprecated(forRemoval = true, since = "v22")
	public String getType() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + " {" + duration() + "ms}";
	}
}