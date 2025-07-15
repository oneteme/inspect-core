package org.usf.inspect.core;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Array.getLength;
import static java.util.Objects.isNull;
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
	
	private static final int MAX_STACK = 5;
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
	
	@Deprecated(forRemoval = true, since = "1.1.0")
	public static void warnStackTrace(String msg) {
		log.warn(msg);
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		var max = min(arr.length, --i+MAX_STACK); //first inspect method call
		while(i<max) {
			log.warn("\tat {}", arr[i++]);
		}
		if(i<arr.length) {
			log.warn("\t...");
		}
	}
	
	public static String prettyURLFormat(String user, String protocol, String host, int port, String path) {
		var s = isNull(user) ? "" : '<' + user + '>';
		if(nonNull(protocol)) {
			s+= protocol + "://";
		}
		if(nonNull(host)) {
			s+= host;
		}
		if(port > 0) {
			s+= ":"+port;
		}
		if(nonNull(path)) {
			if(!path.startsWith("/") && !s.endsWith("/")) { //host & port are null
				s+= '/';
			}
			s+= path;
		}
		return s;
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
}
