package org.usf.inspect.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@AllArgsConstructor
public final class StagePayload {
	
	private String[] args;
	private long[] count;

}
