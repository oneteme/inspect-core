package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonIgnoreProperties("threads")
public abstract class AbstractSession implements CompletableMetric {

	private int requestsMask;
	private String instanceId; //server usage 

	AbstractSession() { }

	AbstractSession(AbstractSession req) {
		this.requestsMask = req.requestsMask;
	}

	public void updateRequestsMask(RequestMask mask) {
		this.requestsMask |= mask.getValue();
	}
	
	public abstract AbstractSession updateContext();
	
	public abstract AbstractSession releaseContext();
}
