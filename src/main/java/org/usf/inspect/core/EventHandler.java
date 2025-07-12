package org.usf.inspect.core;

/**
 * 
 * @author u$f
 *
 */
@FunctionalInterface
public interface EventHandler<T> {
	
	void handle(T obj) throws Exception;
	
	/**
	 * Create a handler that filters the object type before calling the original handler.
	 *
	 **/
	static <T> EventHandler<T> filtredHandler(Class<T> type, EventHandler<T> handler) {
		return new EventHandler<T>() {
			@Override
			public void handle(T obj) throws Exception {
				if(type.isInstance(obj)) {
					handler.handle(type.cast(obj));
				}
			}
		};
	}
}