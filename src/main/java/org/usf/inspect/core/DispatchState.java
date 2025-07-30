package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchState {

	boolean canEmit();
	
	boolean canPropagate();
	
	boolean canDispatch();
	
	default boolean wasCompleted() {
		return false;
	}
	
	/**
	 * Returns a new DispatchState2 instance that:
	 * <ul> 
	 * <li> preserves the previous dispatch state (canDispatch & canPropagate)</li>
	 * <li> disables the emission of new traces (canEmit returns false)</li>
	 * <li> marks this state as completed (wasCompleted returns true)</li>
	 * </ul>
	 * <p>
	 * This is useful to finalize the dispatch process,
	 * preventing further trace emissions while keeping dispatch capabilities.
	 * </p>
	 */
	static DispatchState complete(DispatchState stt) {
		return new DispatchState() {
			
			private final DispatchState state = stt;
						
			@Override
			public boolean canEmit() {
				return false;
			}
			
			@Override
			public boolean canDispatch() {
				return state.canDispatch();
			}
			
			@Override
			public boolean canPropagate() {
				return state.canPropagate();
			}
			
			@Override
			public boolean wasCompleted() {
				return true;
			}
			
			@Override
			public String toString() {
				return state.toString() + ".completed";
			}
		};
	}
}
