package org.usf.traceapi.core;

import static java.lang.Thread.currentThread;
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
	
	public static final Logger log = getLogger(Helper.class.getPackage().getName() + ".TraceAPI");
	
	static String basePackage;

	public static final ThreadLocal<Session> localTrace = new InheritableThreadLocal<>();

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
	
	public static Optional<StackTraceElement> stackTraceElement() {
		if(nonNull(basePackage) && !basePackage.isBlank()) {
			var arr = currentThread().getStackTrace();
			var i = 1; //location, internal call
			while (++i<arr.length && !arr[i].getClassName().startsWith(basePackage));
			if(i<arr.length) {
				return Optional.of(arr[i]);
			}
		}
		return empty();
	}
	
	
	public static void warnNoActiveSession() {
		log.warn("no active session");
		if(nonNull(basePackage) && !basePackage.isBlank()) {
			var arr = currentThread().getStackTrace();
			for(var st : arr) {
				if(st.getClassName().startsWith(basePackage)) {
					log.warn("\tat  {}", st);
				}
			}
		}
	}
}
