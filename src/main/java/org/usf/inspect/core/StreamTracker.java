package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.SessionManager.sessionContextUpdater;

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
public final class StreamTracker {

    @SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(Stream.of(array));
	}

	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	public static <T> Stream<T> parallel(Stream<T> stream) {
		var su = sessionContextUpdater();
    	return isNull(su)
    			? stream.parallel()
    			: stream.parallel().map(c-> {
        			su.updateContext();
        			return c;
        		});
	}
}
