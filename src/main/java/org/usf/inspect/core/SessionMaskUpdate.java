package org.usf.inspect.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@Getter
@RequiredArgsConstructor
public class SessionMaskUpdate implements EventTrace {
	
	private final String id;
	private final boolean main;
	private final int mask;
}