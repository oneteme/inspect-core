package org.usf.inspect.core;

import static java.util.Objects.nonNull;

import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DatabaseRequestStage extends RequestStage {

	private long[] count; // only for BATCH|EXECUTE|FETCH
	
	@Override
	public String prettyFormat() {
		var s = getName();
		if(nonNull(count)) {// !exception
			s += " >> " + Arrays.toString(count);
		}
		return s;
	}
}