package org.usf.inspect.core;

import static java.lang.String.format;

import java.net.URI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assertions {
	
	public static void assertAbsolute(URI uri, String name) {
		if(!uri.isAbsolute()) {
			throw new IllegalArgumentException(format("%s=%s is not absolute", name, uri));
		}
	}
	
	public static int assertPositive(int v, String name) {
		return assertGreaterOrEquals(v, 0, name);
	}
	
	public static int assertGreaterOrEquals(int v, int min, String name) {
		if(v >= min) {
			return v;
		}
		throw new IllegalArgumentException(format("%s='%d' must be >= %d", name, v, min));
	}

	public static <T extends Comparable<T>> T assertBetween(T o, T min, T max, String name) {
		if(o.compareTo(min) < 0) {
			throw new IllegalArgumentException(format("%s='%s' must be >= %s", name, o, min));
		}
		if(o.compareTo(max) > 0) {
			throw new IllegalArgumentException(format("%s='%s' must be <= %s", name, o, max));
		}
		return o;
	}
}
