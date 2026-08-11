package org.usf.inspect.core;

import lombok.RequiredArgsConstructor;

/**
 * Represents whether trace collection and dispatching are enabled.
 */
@RequiredArgsConstructor
public enum DispatchState  {
	
	DISABLE(false, false),
	COLLECT(true, false),
	DISPATCH(true, true);
	
	private final boolean canCollect;
	private final boolean canDispatch;

	/**
	 * Indicates whether traces can be collected in this state.
	 *
	 * @return {@code true} when collection is allowed
	 */
	public boolean canCollect() {
		return canCollect;
	}
	
	/**
	 * Indicates whether traces can be dispatched in this state.
	 *
	 * @return {@code true} when dispatching is allowed
	 */
	public boolean canDispatch() {
		return canDispatch;
	}
}