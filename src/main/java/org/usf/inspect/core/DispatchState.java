package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum DispatchState implements DispatchState2 {
	
	DISABLE(false, false), 
	QUEUE(true, false), 
	DISPATCH(true, true);
	
	final boolean canEmit;
	final boolean canDispatch;
	
	@Override
	public boolean canEmit() {
		return canEmit;
	}
	
	@Override
	public boolean canDispatch() {
		return canDispatch;
	}	
}
