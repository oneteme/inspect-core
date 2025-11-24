package org.usf.inspect.core;

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
public class MainSessionCallback extends AbstractSessionCallback {

	private Instant start; //updated in some cases
	
	public MainSessionCallback(String id, Instant start) {
		super(id);
		this.start = start;
	}
}
