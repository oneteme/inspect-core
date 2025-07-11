package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface EventTraceHandler<T> {
	
	void handle(T obj) throws Exception;
	
	/**
	 * Create a handler that filters the object type before calling the original handler.
	 *
	 **/
	static <T> EventTraceHandler<T> filtredHandler(Class<T> type, EventTraceHandler<T> handler) {
		return new EventTraceHandler<T>() {
			@Override
			public void handle(T obj) throws Exception {
				if(type.isInstance(obj)) {
					handler.handle(type.cast(obj));
				}
			}
		};
	}
}