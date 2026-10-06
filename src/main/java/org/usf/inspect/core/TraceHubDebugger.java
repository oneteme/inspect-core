package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class TraceHubDebugger implements TraceHub {
	
	@Delegate
	private final TraceHub hub;
}
