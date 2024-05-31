package org.usf.traceapi.core;

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
public final class DatabaseRequestStage extends Stage {

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
	public String toString() {
		return super.toString() + " " + Arrays.toString(count);
	}
}