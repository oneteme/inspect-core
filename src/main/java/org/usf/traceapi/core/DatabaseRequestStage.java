package org.usf.traceapi.core;

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

	@Deprecated(forRemoval = true, since = "v22")
	public void setType(String type) {
		setName(type);
	}

	@Deprecated(forRemoval = true, since = "v22")
	public String getType() {
		return getName();
	}
	
	@Override
	public String prettyFormat() {
		var s = getName();
		if(nonNull(count)) {// !exception
			s += " >> " + Arrays.toString(count);
		}
		return s;
	}
}