package org.usf.inspect.core;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.core.SessionContextManager.requireActiveContext;
import static org.usf.inspect.core.SessionContextManager.setActiveContext;

import java.util.Collection;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StreamProxy {

	@SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(stream(array));
	}

	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	public static <T> Stream<T> parallel(Stream<T> stream) {
		if(context().getConfiguration().isEnabled()){
			var ctx = requireActiveContext();
			if(nonNull(ctx)) {
				stream = stream.parallel().map(c-> {
					setActiveContext(ctx);
					return c;
				});
			}
		}
		return stream;
	}
}
