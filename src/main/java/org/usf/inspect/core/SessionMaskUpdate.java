package org.usf.inspect.core;

import java.util.UUID;

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
	
	private final UUID id;
	private final boolean main;
	private final int mask;
}