package org.usf.inspect.core;

import java.time.Instant;

import lombok.Getter;

/**
 * 
 * @author u$f 
 *
 */
@Getter
public class MainSession2 extends AbstractSession2 {

	private final String type;

	public MainSession2(String id, Instant start, String threadName, String type) {
		super(id, start, threadName);
		this.type= type;
	}

	public MainSessionCallback createCallback() {
		return new MainSessionCallback(getId());
	}
}
