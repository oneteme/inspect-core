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
public class LocalRequestCallback extends AbstractRequestCallback {

	private Instant start; //real start
	private ExceptionInfo exception; 

	public LocalRequestCallback(String id) {
		super(id);
	}
}
