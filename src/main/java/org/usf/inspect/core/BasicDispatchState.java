package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum BasicDispatchState implements DispatchState {
	
	DISABLE(false, false, false), 
	PROPAGATE(true, true, false), 
	DISPATCH(true, true, true);
	
	private final boolean canEmit;
	private final boolean propagate;
	private final boolean canDispatch;
	
	@Override
	public boolean canEmit() {
		return canEmit;
	}
	
	@Override
	public boolean canPropagate() {
		return propagate;
	}
	
	@Override
	public boolean canDispatch() {
		return canDispatch;
	}
}