package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum DispatchState  {
	
	DISABLE(false, false),
	COLLECT(true, false),
	DISPATCH(true, true);
	
	private final boolean canCollect;
	private final boolean canDispatch;

	public boolean canCollect() {
		return canCollect;
	}
	
	public boolean canDispatch() {
		return canDispatch;
	}
}