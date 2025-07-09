package org.usf.inspect.core;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assertions {
	
	public static <T> T assertNotNull(T o, String name) {
		if(nonNull(o)) {
			return o;
		}
		throw new IllegalArgumentException(format("%s='%s' must not be in null", name, o));
	}

	public static int assertStrictPositive(int v, String name) {
		if(v > 0) {
			return v;
		}
		throw new IllegalArgumentException(format("%s='%d' must be > 0", name, v));
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

	public static String assertMatches(String s, String regex, String name) {
		if(nonNull(s) && s.matches(regex)) {
			return s;
		}
		throw new IllegalArgumentException(format("%s='%s' must match '%s'", name, s, regex));
	}
	
	public static <T> T assertOneOf(T o, Set<T> set, String name) {
		if(set.contains(o)) {
			return o;
		}
		throw new IllegalArgumentException(format("%s='%s' must be in %s", name, o, set));
	}
}
