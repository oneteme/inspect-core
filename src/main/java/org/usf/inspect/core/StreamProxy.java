package org.usf.inspect.core;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionContextManager.requireActiveContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.util.Collection;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for creating parallel streams that propagate the current session context to each element.
 *
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StreamProxy {

	/**
	 * Creates a parallel stream from the given array elements with session context propagation.
	 *
	 * @param <T> the element type
	 * @param array the elements to stream
	 * @return a parallel stream with session context propagated to each element
	 */
	@SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(stream(array));
	}

	/**
	 * Creates a parallel stream from the given collection with session context propagation.
	 *
	 * @param <T> the element type
	 * @param c the collection to stream
	 * @return a parallel stream with session context propagated to each element
	 */
	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	/**
	 * Converts the given stream to a parallel stream with session context propagated to each element processing step.
	 *
	 * @param <T> the element type
	 * @param stream the stream to parallelize
	 * @return a parallel stream with session context propagation, or the original stream when tracing is disabled
	 */
	public static <T> Stream<T> parallel(Stream<T> stream) {
		if(hub().getConfiguration().isEnabled()){
			var ctx = requireActiveContext();
			if(nonNull(ctx)) {
				stream = stream.parallel().map(c-> {
					setActiveContext(ctx); // propagate context
					return c;
				});
			}
		}
		return stream;
	}
}
