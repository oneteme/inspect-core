package org.usf.inspect.core;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public final class DatabaseRequestStage extends AbstractStage {

	@Deprecated(since = "1.2", forRemoval = true)
	private long[] count;
	@Deprecated(since = "1.2", forRemoval = true)
	private String[] args; // only for BATCH|EXECUTE|FETCH

	public DatabaseRequestStage(UUID requestId, int order) {
		super(requestId, order);
	}
}