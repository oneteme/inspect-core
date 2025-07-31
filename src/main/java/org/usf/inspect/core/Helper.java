package org.usf.inspect.core;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Helper {
	
	private static final String ROOT_PACKAGE;
	
	public static final Logger log;

	static {
		var p = Helper.class.getPackageName();
		ROOT_PACKAGE = p; //root
		log = getLogger(ROOT_PACKAGE + ".collector");
	}
	
	public static String threadName() {
		return currentThread().getName();
	}
	
	public static String extractAuthScheme(List<String> authHeaders) { //nullable
		return nonNull(authHeaders) && authHeaders.size() == 1 //require one header
				? extractAuthScheme(authHeaders.get(0)) : null;
	}
	
	public static String extractAuthScheme(String authHeader) { //nullable
		return nonNull(authHeader) && authHeader.matches("\\w+ .+") 
				? authHeader.substring(0, authHeader.indexOf(' ')) : null;
	}
	
	public static Optional<StackTraceElement> outerStackTraceElement() {
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		return i<arr.length ? Optional.of(arr[i]) : empty();
	}
	
	public static int count(Object o) {
		if(nonNull(o)) {
			if(o instanceof Collection<?> c) {
				return c.size();
			}
			if(o instanceof Map<?,?> m) {
				return m.size();
			}
			if(o.getClass().isArray()) {
				return getLength(o);
			}
		}
		return -1;
	}
	
	public static void warnException(Logger log, Throwable t, String msg, Object... args) {
		log.warn(msg, args);
		log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
		if(log.isDebugEnabled()) {
			while(nonNull(t.getCause()) && t != t.getCause()) {
				t = t.getCause();
				log.warn("  Caused by {} : {}", t.getClass().getSimpleName(), t.getMessage());
			}
		}
	}
}
