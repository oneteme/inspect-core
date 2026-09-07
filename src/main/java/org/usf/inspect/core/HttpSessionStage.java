package org.usf.inspect.core;

import java.util.UUID;

/**
 * 
 * @author u$f
 *
 */
public final class HttpSessionStage extends AbstractStage {

	public HttpSessionStage(UUID requestId, long order) {
		super(requestId, order);
	}
}