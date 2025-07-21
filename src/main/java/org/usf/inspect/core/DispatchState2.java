package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
public interface DispatchState2 {

	boolean canEmit();
	
	boolean canDispatch();
	
	default boolean wasCompleted() {
		return false;
	}
	
	default DispatchState2 complete() {
		return new DispatchState2() {
			
			@Override
			public boolean canEmit() {
				return false;
			}
			
			@Override
			public boolean canDispatch() {
				return DispatchState2.this.canDispatch();
			}
			
			@Override
			public boolean wasCompleted() {
				return true;
			}
		};
	}
}
