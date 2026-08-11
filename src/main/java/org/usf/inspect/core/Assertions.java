package org.usf.inspect.core;

import static java.lang.String.format;

import java.net.URI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility methods for validating inspect arguments.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assertions {
	
	/**
	 * Ensures that the given URI is absolute.
	 *
	 * @param uri the URI to validate
	 * @param name the argument name
	 */
	public static void assertAbsolute(URI uri, String name) {
		if(!uri.isAbsolute()) {
			throw new IllegalArgumentException(format("%s=%s is not absolute", name, uri));
		}
	}
	
	/**
	 * Ensures that the given value is greater than or equal to zero.
	 *
	 * @param v the value to validate
	 * @param name the argument name
	 * @return the validated value
	 */
	public static int assertPositive(int v, String name) {
		return assertGreaterOrEquals(v, 0, name);
	}
	
	/**
	 * Ensures that the given value is greater than or equal to the specified minimum.
	 *
	 * @param v the value to validate
	 * @param min the minimum allowed value
	 * @param name the argument name
	 * @return the validated value
	 */
	public static int assertGreaterOrEquals(int v, int min, String name) {
		if(v >= min) {
			return v;
		}
		throw new IllegalArgumentException(format("%s='%d' must be >= %d", name, v, min));
	}

	/**
	 * Ensures that the given comparable value is within the specified bounds.
	 *
	 * @param <T> the comparable value type
	 * @param o the value to validate
	 * @param min the minimum allowed value
	 * @param max the maximum allowed value
	 * @param name the argument name
	 * @return the validated value
	 */
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
