package org.usf.inspect.core;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static org.usf.inspect.core.SessionManager.requireCurrentSession;

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
public final class StreamMonitor {

    @SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(stream(array));
	}

	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	public static <T> Stream<T> parallel(Stream<T> stream) {
		var ses = requireCurrentSession();
    	return isNull(ses)
    			? stream.parallel()
    			: stream.parallel().map(c-> {
        			ses.updateContext();
        			return c;
        		});
	}
}
