package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum DispatchState {
	
	DISABLE(false, false), 
	QUEUE(true, false), 
	DISPATCH(true, false), 
	COMPLETE(false, true);
	
	final boolean canEmit;
	final boolean canDispatch;
}
