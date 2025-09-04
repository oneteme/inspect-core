package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class StageArgsHolder {

	private DatabaseAction action;
	private DatabaseRequestStage stage;
	private long[] count;

	public void set(DatabaseAction action, DatabaseRequestStage stage, long[] count) {
		this.action = action;
		this.stage = stage;
		this.count = count;
	}
	
	public void clear() {
		this.action = null;
		this.stage = null;
		this.count = null;
	}
}
