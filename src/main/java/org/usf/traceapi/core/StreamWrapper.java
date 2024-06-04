package org.usf.traceapi.core;

import static java.util.Objects.isNull;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.warnNoActiveSession;

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
public final class StreamWrapper {

    @SafeVarargs
	public static <T> Stream<T> parallelStream(T... array) {
		return parallel(Stream.of(array));
	}

	public static <T> Stream<T> parallelStream(Collection<T> c) {
		return parallel(c.stream());
	}

	public static <T> Stream<T> parallel(Stream<T> stream) {
		var s = localTrace.get();
        if(isNull(s)) {
        	warnNoActiveSession();
        	return stream.parallel();
        }
		return stream.parallel().map(c-> { //lock session !?
					if(s != localTrace.get()) { // null || previous session
						localTrace.set(s);
					}
					return c;
				});
				//.onClose(()->localTrace.remove())
	}
}
