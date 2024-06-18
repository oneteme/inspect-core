package org.usf.traceapi.core;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
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
	
	private static final int MAX_STACK = 5; //skipped
	private static final String ROOT_PACKAGE;
	
	public static final Logger log;

	public static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();
	
	static {
		var p = Helper.class.getPackageName();
		ROOT_PACKAGE = p.substring(0, p.lastIndexOf(".")); //root
		log = getLogger(ROOT_PACKAGE + ".Collector");
	}
	
	public static void setThreadLocalSession(Session s) {
		if(localTrace.get() != s) { // null || local previous session
			localTrace.set(s);
		}
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
	
	public static <T> Optional<T> newInstance(Class<? extends T> clazz) {
		try {
			return Optional.of(clazz.getDeclaredConstructor().newInstance());
		} catch (Exception e) {
			log.warn("cannot instantiate class " + clazz.getName(), e);
			return empty();
		}
	}
	
	public static Optional<StackTraceElement> outerStackTraceElement() {
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		return i<arr.length ? Optional.of(arr[i]) : empty();
	}
	
	public static void warnNoActiveSession(Object o) {
		log.warn("no active session: {}", o);
		var arr = currentThread().getStackTrace();
		var i = 1; //skip this method call
		while(i<arr.length && arr[i].getClassName().startsWith(ROOT_PACKAGE)) {i++;}
		var max = min(arr.length, --i+MAX_STACK); //first JQuery method call
		while (i<max) {
			log.warn("\tat {}", arr[i++]);
		}
		if(i<arr.length) {
			log.warn("\t...");
		}
	}
	
	public static String prettyURLFormat(String user, String protocol, String host, int port, String path) {
		var s = isNull(path) ? "" : '<' + user + '>';
		s += protocol + "://" + host;
		if(port > 0) {
			s+= ':'+port;
		}
		if(nonNull(path)) {
			if(!path.endsWith("/")) {
				s+= '/';
			}
			s+= path;
		}
		return s;
	}
}
