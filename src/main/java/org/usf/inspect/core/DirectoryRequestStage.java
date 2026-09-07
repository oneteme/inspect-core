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
public class DirectoryRequestStage extends AbstractStage {

	@Deprecated(since = "1.2", forRemoval = true)
	private String[] args;
	
	public DirectoryRequestStage(UUID requestId, int order) {
		super(requestId, order);
	}
}
